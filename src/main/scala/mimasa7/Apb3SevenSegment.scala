package mimasa7

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3Gpio, Apb3SlaveFactory}

import scala.language.postfixOps


object SevenSegmentHexData {
  def apply(): Vec[Bits] = {
    /*
      *     +---- 5 ----+
      *     |           |
      *     6           4
      *     |           |
      *     +---- 7 ----+
      *     |           |
      *     0           2
      *     |           |
      *     +---- 1 ----+ (3 dot)
      * */
    Vec(Array[Bits](xs =
      B"1110_1110", // 0
      B"0010_1000", // 1
      B"1100_1101", // 2
      B"0110_1101", // 3
      B"0010_1011", // 4
      B"0110_0111", // 5
      B"1110_0111", // 6
      B"0010_1100", // 7
      B"1110_1111", // 8
      B"0110_1111", // 9
      B"1010_1111", // A
      B"1110_0011", // b
      B"1100_0001", // C
      B"1110_1001", // d
      B"1100_0111", // E
      B"1000_0111"  // F
    ))
  }
}


case class Apb3SevenSegmentConfig(
                                   apb3Config: Apb3Config = Apb3Config(addressWidth = 4,dataWidth = 32),
                                   segmentCount: Int
                                 )

class Apb3SevenSegmentCtrl(config: Apb3SevenSegmentConfig) extends Component {
  val io = new Bundle {
    val apb  = slave(Apb3(config.apb3Config))
    val enableBus = out Bits(config.segmentCount bits)
    val dataBus = out Bits(8 bits)
  }

  private val SEGMENT_MAPPING = SevenSegmentHexData()

  private val segmentIndexBits = log2Up(config.segmentCount)
  private val segmentIndex = Reg(UInt(segmentIndexBits bits)) init 0
  private val counter = Reg(UInt(16 bits)) init 0
  counter := counter + 1

  // The segment LEDs cannot switch fast enough to keep up with the FPGAs speed.
  when (counter === 0) {
    when(segmentIndex === config.segmentCount - 1) {
      segmentIndex := 0
    } otherwise {
      segmentIndex := segmentIndex + 1
    }
  }

  // active low
  io.enableBus := ~(B"1" << segmentIndex)

  private val segmentStates = Vec(Reg(Bits(8 bits)), config.segmentCount)
  //segmentStates.zipWithIndex.foreach{ case (reg, i) => reg init B(i + 1, 8 bits) }
  //segmentStates.zipWithIndex.foreach{ case (reg, i) => reg init B"0000_0111" }
  segmentStates.zipWithIndex.foreach{ case (reg, i) => reg init SEGMENT_MAPPING(i) }

  // active low
  io.dataBus := ~segmentStates(segmentIndex)


  // registers
  private val registers = Vec(Reg(UInt(32 bits)), config.segmentCount)
  registers.foreach{ _ init U(0, 32 bits) }
  private val factory = Apb3SlaveFactory(io.apb)

  for (i <- 0 until config.segmentCount)
    factory.driveAndRead(registers(i), i * 4)

  for (i <- 0 until config.segmentCount) {
    val idx = Min(registers(i), U(SEGMENT_MAPPING.length - 1)).resized
    segmentStates(i) := SEGMENT_MAPPING(idx)
  }
}

object Apb3SevenSegmentCtrl {
  def apply(config: Apb3SevenSegmentConfig): Apb3SevenSegmentCtrl = new Apb3SevenSegmentCtrl(config)
}

case class SevenSegmentHexConfig(segmentCount: Int, frequency: HertzNumber)

class SevenSegmentHex(config: SevenSegmentHexConfig) extends Component {
  private val SevenSegmentHexConfig(segmentCount, frequency) = config
  private val TOTAL_BIT_COUNT = segmentCount * 4
  private val CYCLE_COUNT = (ClockDomain.current.frequency.getValue / frequency).toInt

  val io = new Bundle {
    // n digit hex number to be displayed
    val number = slave Stream UInt(TOTAL_BIT_COUNT bits)
    // controlling signals for the display
    val enableBus = out Bits (segmentCount bits)
    val dataBus = out Bits (8 bits)
  }

  // load io.number into the digit registers
  private val digits = Vec(RegInit(U(0xF, 4 bits)), segmentCount)
  io.number.ready := True

  when (io.number.fire) {
    for ((targetValue, index) <- digits.zipWithIndex) {
      val offset = (digits.length - 1 - index) * 4
      targetValue := io.number.payload(offset, 4 bits)
    }
  }

  // display loop
  private val data = SevenSegmentHexData()
  private val cycleCounter = Counter(0 until CYCLE_COUNT, inc = True)
  private val digitIndex = Counter(0 until segmentCount, inc = cycleCounter === 0)

  // active low
  io.enableBus := ~(B"1" << digitIndex)
  io.dataBus := ~data(digits(digitIndex))
}

object SevenSegmentHex {
  def apply(config: SevenSegmentHexConfig): SevenSegmentHex = new SevenSegmentHex(config)
}