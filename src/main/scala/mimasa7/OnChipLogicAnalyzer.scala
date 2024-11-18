package mimasa7

import spinal.core._
import spinal.lib._
import spinal.lib.com.jtag.sim.JtagDriver
import spinal.lib.fsm._
import spinal.lib.com.jtag.{Jtag, JtagTap, JtagTapInstructionCtrl}
import spinal.sim.SimManagerContext

import scala.collection.mutable.ArrayBuffer


case class BitReadPort() extends Bundle with IMasterSlave {
  val address = master Stream UInt(24 bits)
  val data = slave Stream Bool()

  override def asMaster(): Unit = {
    address.asMaster()
    data.asSlave()
  }

  override type RefOwnerType = this.type
}

class ShiftRegister[T <: Data](dataType: HardType[T], depth: Int) extends Component {
  val io = new Bundle {
    val push = slave Stream dataType()
    val bitReadPort = slave(BitReadPort())
  }

  private val buffer = Vec.fill(depth)(Reg(dataType()))

  io.push.ready := True
  private val doShift = io.push.fire

  when (doShift) {
    for (i <- 1 until buffer.length)
      buffer(i) := buffer(i - 1)

    buffer(0) := io.push.payload
  }

  val virtualDataWidth = 1 << log2Up(dataType.getBitsWidth)
  // virtualDataWidth being a power of 2 simplifies the bit-wise access logic
  require(isPow2(virtualDataWidth))

  val wordAddress = io.bitReadPort.address.payload / virtualDataWidth
  val bitIndex = io.bitReadPort.address.payload % virtualDataWidth

  val readData = StreamFifo(Bool(), depth = 1)
  io.bitReadPort.address.ready := readData.io.push.ready
  readData.io.push.valid := io.bitReadPort.address.valid
  readData.io.push.payload := (buffer(wordAddress.resized).asBits >> bitIndex.resized).lsb

  readData.io.pop >> io.bitReadPort.data
}

case class CaptureInterface() extends Bundle {
  // Notify the user if data has been captured.
  val didCapture = out Bool()
  // Let the user reset the state of the capture module.
  val doClear = in Bool()
  // Let the user access the captured bits.
  val bitReadPort = slave(BitReadPort())
}

class Capture[T <: Data](dataType: HardType[T], condition: T => Bool, fifoDepth: Int, captureCount: Int) extends Component {
  require(captureCount < fifoDepth)

  val io = new Bundle {
    // the signal to potentially capture
    val signal = in(dataType())
    // user control
    val captureInterface = CaptureInterface()
  }

  val trigger = condition(io.signal)
  val shiftRegister = new ShiftRegister(dataType, fifoDepth)
  shiftRegister.io.bitReadPort <> io.captureInterface.bitReadPort

  // we push data into the shift register by default
  val pushData = Bool()
  shiftRegister.io.push.payload := io.signal
  shiftRegister.io.push.valid := pushData

  val didCapture = Bool()
  io.captureInterface.didCapture := didCapture

  val counter = Counter(0 until captureCount)

  val fsm = new StateMachine {
    // we push data into the shift register by default
    pushData := True
    didCapture := False

    val READY: State = new State with EntryPoint {
      whenIsActive {
        when (trigger) {
          goto (CAPTURING)
        }
      }
    }

    // We will be in this state for exactly captureCount cycles which means
    // that we capture captureCount frames AFTER the condition was triggered.
    val CAPTURING: State = new State {
      whenIsActive {
        counter.increment()

        when (counter.willOverflow) {
          goto(DONE)
        }
      }
    }

    val DONE: State = new State {
      whenIsActive {
        pushData := False
        didCapture := True

        // Wait until the user actively clears, then reset the state.
        when (io.captureInterface.doClear) {
          counter.clear()
          goto(READY)
        }
      }
    }
  }
}

class OnChipLogicAnalyzer(jtag: Jtag) extends Area {
  private val captureInterfaces = ArrayBuffer[CaptureInterface]()

  def capture[T <: Data](signal: T, condition: T => Bool, fifoDepth: Int = 32, captureCount: Int = 31): Unit = {
    val cap = new Capture(HardType(signal), condition, fifoDepth, captureCount)
    cap.io.signal <> signal
    captureInterfaces.append(cap.io.captureInterface)
  }

  Component.current.afterElaboration {
    val ctrl = new ClockingArea(ClockDomain(jtag.tck, ClockDomain.current.readResetWire)) {
      val tap = new JtagTap(jtag, 8)
      val idcodeArea = tap.idcode(B"x1234ABCD")(instructionId = 4)

      captureInterfaces.zipWithIndex.foreach { case (captureInterface, index) =>
        // TODO: What was this?
        //val parent = Component.current.parent
        //require(parent != null, "OnChipLogicAnalyzer cannot be a top level component!")

        // Control registers that are controlled by the TAP
        val didCapture = RegInit(False)
        val doClear = RegInit(False)
        val address = Reg(UInt(24 bits)) randBoot()
        val data = Reg(Bool()) randBoot()

        // captureInterface lives the in the parents clock domain
        didCapture := BufferCC(captureInterface.didCapture)
        captureInterface.doClear := BufferCC(doClear)
        // We ignore certain parts of the Stream protocols because Jtag is too slow anyways to cause problems.
        captureInterface.bitReadPort.address.payload := BufferCC(address)
        captureInterface.bitReadPort.address.valid := True
        data := BufferCC(captureInterface.bitReadPort.data.payload &
          captureInterface.bitReadPort.data.valid)

        // Register control registers with the TAP
        val id = 5 + index * 4
        tap.read(didCapture)(instructionId = id + 0)
        tap.write(doClear)(instructionId = id + 1)
        tap.write(address)(instructionId = id + 2)
        tap.read(data)(instructionId = id + 3)

        SpinalInfo(s"Built new capture with didCapture, doClear, address and data registers at Jtag instruction ids $id to ${id + 3}")
      }
    }
  }
}

class OnChipLogicAnalyzerTest extends Component {
  val io = new Bundle {
    val x = in UInt(8 bits)
    val jtag = slave(Jtag())
  }

  val ocla = new OnChipLogicAnalyzer(io.jtag) {
    capture(io.x, (y: UInt) => y === 123)
  }
}

import spinal.core.sim._

class OnChipLogicAnalyzerJtagDriver(_jtag: Jtag, _clockPeriod: TimeNumber) extends JtagDriver(_jtag, _clockPeriod) {
  def toBooleanSeq(value: Int, bitCount: Int): Seq[Boolean] = {
    (0 until bitCount).map { i =>
      ((value >> i) & 1) == 1
    }
  }

  def toInt(seq: Seq[Boolean]): Int = {
    seq.zipWithIndex.map { case (b, i) =>
      if (b) 1 << i else 0
    }.fold(0)((a, b) => a | b)
  }

  def doReadWriteDR(id: Int, drWidth: Int, newDRValue: Option[Int] = None): Int = {
    val idSeq = toBooleanSeq(id, 8)
    val newDRSeq = toBooleanSeq(newDRValue.getOrElse(0), drWidth)

    // RESET -> SHIFT-IR
    doTmsSeq(Seq(false, true, true, false, false))
    // write instruction id
    doScanChain(idSeq, flipTms = true)
    // goto RESET
    doResetTap()
    // RESET -> SHIFT-DR
    doTmsSeq(Seq(false, true, false, false))
    // read DR
    val drSeq = doScanChain(newDRSeq, flipTms = true)
    val drValue = toInt(drSeq)

    drValue
  }

  def readIdCode(): Int = doReadWriteDR(0x4, 32)

  def wasCaptured(): Boolean = doReadWriteDR(0x5, 32) == 1

  def readBitAtAddress(bitAddress: Int): Boolean = {
    doReadWriteDR(0x7, 24, Some(bitAddress))
    val result = doReadWriteDR(0x8, 1) == 1
    result
  }

  def doReadCaptured(): Seq[Int] = {


    ???
  }
}

object OnChipLogicAnalyzerTest {
  def main(args: Array[String]): Unit = {
    SimConfig.withVcdWave.compile(new OnChipLogicAnalyzerTest()).doSim { dut =>
      val jtagClockPeriod = 1
      val period = TimeNumber(SimManagerContext.current.manager.timePrecision * 2)
      val jtagClockDomain = ClockDomain(dut.io.jtag.tck)
      val mainClockDomain = dut.clockDomain
      val jtagDriver = new OnChipLogicAnalyzerJtagDriver(dut.io.jtag, period)

      def reset(): Unit = {
        mainClockDomain.assertReset()

        for (_ <- 0 until 10) {
          mainClockDomain.clockToggle()
          jtagClockDomain.clockToggle()
          sleep(1)
        }

        mainClockDomain.deassertReset()
      }

      jtagDriver.doResetTap()
      reset()

      val clockThread = fork {
        for (i <- 0 until 10000) {
          jtagClockDomain.clockToggle()
          mainClockDomain.clockToggle()
          sleep(jtagClockPeriod)
        }
      }

      val mainThread = fork {
        for (i <-0 until 256) {
          dut.io.x #= i
          sleep(jtagClockPeriod * 2)
        }
      }

      val jtagThread = fork {
        jtagDriver.doResetTap()
        val idCode = jtagDriver.readIdCode()
        println(s"IDCODE = 0x${idCode.toHexString}")

        for (_ <- 0 until 1000)
          sleep(jtagClockPeriod * 2)

        //println(s"was captured: ${jtagDriver.wasCaptured()}")

        for (i <- 0 until 1000) {
          //println(s"was captured: ${jtagDriver.wasCaptured()}")
          sleep(jtagClockPeriod * 2)
        }
      }

      jtagThread.join()
      clockThread.join()
      mainThread.join()
    }
  }
}
