package mimasa7

import spinal.core._
import spinal.lib._
import spinal.lib.com.jtag._
import spinal.lib.misc.HexTools

class MimasA7JtagExample extends Component {
  val io = new Bundle {
    val asyncReset = in Bool()
    val mainClk = in Bool()
    val jtag = slave(Jtag())
    val sevenEnable = out Bits (4 bits)
    val sevenData = out Bits (8 bits)
    val leds = out Bits (8 bits)
  }

  val mainClockDomain = new ClockDomain(
    io.mainClk, io.asyncReset, frequency = FixedFrequency(35 MHz)
  )

  val jtagClockDomain = new ClockDomain(
    io.jtag.tck
  )

  val myTap = new MimasA7JtagTap()
  io.jtag <> myTap.io.jtag
  io.leds <> myTap.io.leds

  myTap.ctrl.tap.fsm.state.addAttribute("fsm_encoding", "none")

  val mainClockingArea = new ClockingArea(mainClockDomain) {
    val sevenHex = SevenSegmentHex(SevenSegmentHexConfig(4, 100 Hz))
    io.sevenEnable := sevenHex.io.enableBus
    io.sevenData := sevenHex.io.dataBus

    /*
    val slowArea = new SlowArea(1 Hz) {
      val counter = Counter(0 until 0xffff, inc = True)
      sevenHex.io.number.valid := True
      sevenHex.io.number.payload := counter
    }*/

    sevenHex.io.number.valid := True
    // There seems to be some weird pruning happening when we don't use internalState
    //jtagClockingArea.myTap.io.internalState.addTag(???)
    //sevenHex.io.number.payload := BufferCC(myTap.io.internalState).resized
    sevenHex.io.number.payload := 0
  }
}

object MimasA7JtagExample {
  def main(args: Array[String]): Unit = {
    val spinalConfig = SpinalConfig().withoutEnumString()
    SpinalVerilog(spinalConfig)(new MimasA7JtagExample())
  }
}




class JtagBram extends Component {
  val io = new Bundle {
    val jtag    = slave(Jtag())
    val ramAddress = out UInt(32 bits)
  }

  val ctrl = new ClockingArea(ClockDomain(io.jtag.tck)) {
    val ramAddress = Reg(UInt(32 bits))

    val tap = new JtagTap(io.jtag, 8)
    val idcodeArea0 = tap.idcode(B"x87654321")(instructionId = 4)
    val ramAddressArea = tap.write(ramAddress)(instructionId = 7)

    io.ramAddress := ramAddress
  }
}

class BramTest extends Component {
  val io = new Bundle {
    val asyncReset = in Bool()
    val mainClk = in Bool()
    val jtag = slave(Jtag())
    val sevenEnable = out Bits (4 bits)
    val sevenData = out Bits (8 bits)
    val leds = out Bits (8 bits)
  }

  // default drivers
  io.leds := B"b10101010"

  private val mainClockDomain = new ClockDomain(io.mainClk, io.asyncReset, frequency = ClockDomain.current.frequency)
  private val jtagClockDomain = new ClockDomain(io.jtag.tck)

  private val jtagClockingArea = new ClockingArea(jtagClockDomain) {
    val jtag = new JtagBram()
    io.jtag <> jtag.io.jtag
  }

  private val mainClockingArea = new ClockingArea(mainClockDomain) {
    // RAM
    val onChipRamSize = 8 kB
    val onChipRamHexFile = "src/main/c/murax/sevensegment/build/seven_segment.hex"
    // TODO: 16 bits?
    val ram = Mem(Bits(16 bits), onChipRamSize / 4)
    ram.addAttribute("ram_style", "block")
    //HexTools.initRam(ram, onChipRamHexFile, 0x80000000L, allowOverflow = true)
    HexTools.initRam(ram, onChipRamHexFile, 0x80000000L, allowOverflow = true)

    // display
    val sevenHex = SevenSegmentHex(SevenSegmentHexConfig(4, 100 Hz))
    io.sevenEnable := sevenHex.io.enableBus
    io.sevenData := sevenHex.io.dataBus

    val address = BufferCC(jtagClockingArea.jtag.io.ramAddress)
    val ramValue = ram(address.resized)

    sevenHex.io.number.payload := ramValue.asUInt
    sevenHex.io.number.valid := True
  }
}

object BramTest {
  def main(args: Array[String]): Unit = {
    val spinalConfig = SpinalConfig(
      defaultClockDomainFrequency = FixedFrequency(35 MHz)
    ).withoutEnumString()
    SpinalVerilog(spinalConfig)(new BramTest())
  }
}