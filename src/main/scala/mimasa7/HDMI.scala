package mimasa7

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.s7.{ODDR, PLLE2_BASE}
import spinal.lib.graphic._
import spinal.lib.graphic.hdmi.TmdsEncoder
import spinal.lib.graphic.vga._

class PixelGenerator(vgaCd : ClockDomain) extends Component {
  val io = new Bundle {
    val frameStart = in Bool()
    val pixels = master Stream Rgb(RgbConfig(8, 8, 8))
  }

  new ClockingArea(vgaCd) {
    val counter = Counter(0 to 0xffffff)

    when (io.frameStart) {
      counter.clear()
    } elsewhen (io.pixels.fire) {
      counter.increment()
    }

    io.pixels.valid := True
    io.pixels.payload.r := counter(23 downto 16)
    io.pixels.payload.g := counter(15 downto 8)
    io.pixels.payload.b := counter(7 downto 0)
  }
}

class VgaToHdmiXc7(vgaCd : ClockDomain, hdmiCd : ClockDomain) extends Component {
  val io = new Bundle {
    val vga = slave(Vga(RgbConfig(8, 8, 8)))
    val gpdi_dp, gpdi_dn = out Bits (4 bits)
  }

  // VGA clock domain
  val TMDS_red, TMDS_green, TMDS_blue = Bits(10 bits)
  val bCd = io.vga.vSync ## io.vga.hSync
  val encode_R = vgaCd(TmdsEncoder(VD = io.vga.color.r, CD = B"00", VDE = io.vga.colorEn, TMDS = TMDS_red))
  val encode_G = vgaCd(TmdsEncoder(VD = io.vga.color.g, CD = B"00", VDE = io.vga.colorEn, TMDS = TMDS_green))
  val encode_B = vgaCd(TmdsEncoder(VD = io.vga.color.b, CD = bCd, VDE = io.vga.colorEn, TMDS = TMDS_blue))

  // HDMI clock domain
  val ctr_mod5 = hdmiCd(Reg(UInt(3 bits)) randBoot())
  val shift_ld = hdmiCd(Reg(Bool()) randBoot())

  shift_ld := ctr_mod5 === 4
  ctr_mod5 := ((ctr_mod5 === 4) ? U"3'd0" | (ctr_mod5 + 1))

  val shift_R, shift_G, shift_B, shift_C = hdmiCd(Reg(Bits(10 bits)))

  // Clock domain crossing! TODO: Is it okay?
  shift_R := (shift_ld ? TMDS_red | shift_R(9 downto 2).resized).addTag(crossClockDomain)
  shift_G := (shift_ld ? TMDS_green | shift_G(9 downto 2).resized).addTag(crossClockDomain)
  shift_B := (shift_ld ? TMDS_blue | shift_B(9 downto 2).resized).addTag(crossClockDomain)
  shift_C := (shift_ld ? B"10'h3e0" | shift_C(9 downto 2).resized).addTag(crossClockDomain)

  val ddr3p = hdmiCd(ODDR(Q = io.gpdi_dp(3), D1 = shift_C(0), D2 = shift_C(1)))
  val ddr2p = hdmiCd(ODDR(Q = io.gpdi_dp(2), D1 = shift_R(0), D2 = shift_R(1)))
  val ddr1p = hdmiCd(ODDR(Q = io.gpdi_dp(1), D1 = shift_G(0), D2 = shift_G(1)))
  val ddr0p = hdmiCd(ODDR(Q = io.gpdi_dp(0), D1 = shift_B(0), D2 = shift_B(1)))

  val ddr3n = hdmiCd(ODDR(Q = io.gpdi_dn(3), D1 = ~shift_C(0), D2 = ~shift_C(1)))
  val ddr2n = hdmiCd(ODDR(Q = io.gpdi_dn(2), D1 = ~shift_R(0), D2 = ~shift_R(1)))
  val ddr1n = hdmiCd(ODDR(Q = io.gpdi_dn(1), D1 = ~shift_G(0), D2 = ~shift_G(1)))
  val ddr0n = hdmiCd(ODDR(Q = io.gpdi_dn(0), D1 = ~shift_B(0), D2 = ~shift_B(1)))
}

case class MimasA7HDMI() extends Bundle {
  val clk_p = Bool()
  val clk_n = Bool()
  val tmds_p = Bits(3 bits)
  val tmds_n = Bits(3 bits)
}

class HdmiTest() extends Component {
  val io = new Bundle {
    val hdmi = out(MimasA7HDMI())
    val led = out Bool()
  }

  // TODO: Enough bits?
  val timings = VgaTimings(12)
  // This implies a 25MHz VGA clk and a 125MHz HDMI clk
  // https://numato.com/kb/hdmi-output-example-design-using-vivado-for-mimas-a7-fpga-development-board/
  timings.setAs_h640_v480_r60

  // Assume 100 MHz input clock.
  // 125 MHz = 100 MHz * 10 / 8
  //  25 MHz = 100 MHz * 10 / 40
  val pll = PLLE2_BASE(
    clkOut_Mult = 10,
    clkOut1_Divide = 8,
    clkOut0_Divide = 40
  )

  pll.CLKFBIN := pll.CLKFBOUT // internal loop back
  pll.CLKIN1 := ClockDomain.current.readClockWire
  pll.RST := False
  pll.PWRDWN := False

  val reset = ClockDomain.current.readResetWire
  val hdmiCd = ClockDomain(pll.CLKOUT0, reset, frequency = FixedFrequency(125 MHz))
  val vgaCd = ClockDomain(pll.CLKOUT1, reset, frequency = FixedFrequency(25 MHz))

  val pixelGenerator = new PixelGenerator(vgaCd)
  val vgaToHdmi = new VgaToHdmiXc7(vgaCd, hdmiCd)

  new ClockingArea(vgaCd) {
    val vgaCtrl = new VgaCtrl(RgbConfig(8, 8, 8))
    vgaCtrl.io.timings <> timings

    pixelGenerator.io.pixels <> vgaCtrl.io.pixels
    pixelGenerator.io.frameStart := vgaCtrl.io.frameStart

    vgaCtrl.io.vga <> vgaToHdmi.io.vga

    io.hdmi.clk_n := vgaToHdmi.io.gpdi_dn(3)
    io.hdmi.clk_p := vgaToHdmi.io.gpdi_dp(3)

    // TODO: Check RGB order
    for (i <- 0 until 3) {
      io.hdmi.tmds_n(i) := vgaToHdmi.io.gpdi_dn(i)
      io.hdmi.tmds_p(i) := vgaToHdmi.io.gpdi_dp(i)
    }
  }

  new SlowArea(1 Hz) {
    val ledBuf = RegInit(False)
    ledBuf := ~ledBuf
    io.led := ledBuf
  }
}

object HdmiTest {
  def main(args: Array[String]): Unit = {
    val config = SpinalConfig(
      defaultClockDomainFrequency = FixedFrequency(100 MHz)
    ).withoutEnumString()
    SpinalVerilog(config)(new HdmiTest())
  }
}