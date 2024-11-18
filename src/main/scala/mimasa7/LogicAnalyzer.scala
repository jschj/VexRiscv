package mimasa7

import spinal.core._
import spinal.core.sim.SimConfig
import spinal.lib._
import spinal.lib.fsm._
import spinal.lib.com.jtag.{Jtag, JtagTap, JtagTapInstructionCtrl}

import scala.collection.mutable.ArrayBuffer

class SignalCapture[T <: Data](dataType: HardType[T], fifoDepth: Int, condition: T => Bool) extends Component {
  val io = new Bundle {
    // the signal to capture
    val signal = in(dataType())
    // captured data
    val popCapturedData = master Stream dataType()
  }

  val trigger = condition(io.signal)
  val signalBuffer = RegNext(io.signal)

  val fifo = StreamFifo(dataType(), depth = fifoDepth)
  fifo.io.push.valid := False
  fifo.io.push.payload := signalBuffer

  io.popCapturedData.valid := False
  io.popCapturedData.payload := fifo.io.pop.payload
  fifo.io.pop.ready := False

  val fsm = new StateMachine {
    val READY: State = new State with EntryPoint {
      whenIsActive {
        when (trigger) {
          goto(CAPTURING)
        }
      }
    }

    val CAPTURING: State = new State {
      whenIsActive {
        when (fifo.io.availability === 0) {
          goto(DEQUEUING)
        } otherwise {
          fifo.io.push.valid := True
        }
      }
    }

    val DEQUEUING: State = new State {
      whenIsActive {
        io.popCapturedData.valid := fifo.io.pop.valid
        fifo.io.pop.ready := io.popCapturedData.ready

        when (fifo.io.occupancy === 0) {
          goto(READY)
        }
      }
    }
  }
}

class SignalToJtagAdapter[T <: Data](dataType: HardType[T], depth: Int) extends Component {
  val io = new Bundle {
    val push = slave Stream dataType()
    val ctrl = slave(JtagTapInstructionCtrl())
    val status = out UInt(log2Up(depth + 1) bits)
  }

  val bitCount = dataType.getBitsWidth

  val fifo = StreamFifo(dataType, depth)
  io.push >> fifo.io.push
  io.status := fifo.io.occupancy
  fifo.io.pop.ready := False

  new Area {
    val data = Mux(fifo.io.pop.valid, fifo.io.pop.payload, B(0, bitCount bits).as(dataType))
    val counter = Reg(UInt(log2Up(data.getBitsWidth) bits))

    when (io.ctrl.enable) {
      when (io.ctrl.capture) {
        counter := 0
      }

      when (io.ctrl.shift) {
        counter := counter + 1

        // Will overflow? Pop the next piece of data from the fifo!
        when (~counter === 0) {
          fifo.io.pop.ready := True
        }
      }
    }

    io.ctrl.tdo := B(data)(counter)
  }
}

case class LogicAnalyzerConfig(fifoDepth: Int)

class LogicAnalyzer(config: LogicAnalyzerConfig, jtag: Jtag) extends Area {
  private val toBuild = ArrayBuffer[JtagTap => Unit]()

  def capture[T <: Data](signal: T, condition: T => Bool): Unit = {
    val index = toBuild.length

    toBuild.append({ tap =>
      val statusInstructionId = index * 2 + 5
      val adapterInstructionId = statusInstructionId + 1

      val captureArea = new ClockingArea(ClockDomain.current) {
        val capture = new SignalCapture(HardType(signal), config.fifoDepth, condition)
      }

      val adapter = new SignalToJtagAdapter(HardType(signal), config.fifoDepth)

      captureArea.capture.io.signal := signal
      //captureArea.capture.io.popCapturedData >> adapter.io.push

      val fifoCC = StreamFifoCC(HardType(signal), 1, captureArea.clockDomain, adapter.clockDomain)
      captureArea.capture.io.popCapturedData >> fifoCC.io.push
      fifoCC.io.pop >> adapter.io.push

      tap.read(adapter.io.status, light = true)(instructionId = statusInstructionId)
      tap.map(adapter.io.ctrl, instructionId = adapterInstructionId)

      SpinalInfo(s"[LogicAnalyzer]: Capturing signal ${signal.name} of type ${NamedType(signal).name}")
      SpinalInfo(s"[LogicAnalyzer]: Status instruction id $statusInstructionId")
      SpinalInfo(s"[LogicAnalyzer]: Adapter instruction id $adapterInstructionId")
    })
  }

  private var built = false

  private def build(): Unit = {
    if (built)
      return

    built = true

    val jtagWithReset = ClockDomain(jtag.tck, ClockDomain.current.readResetWire)

    val ctrl = new ClockingArea(jtagWithReset) {
      val tap = new JtagTap(jtag, 8)
      val idcodeArea = tap.idcode(B"x1234ABCD")(instructionId = 4)

      toBuild.foreach(func => func(tap))
    }
  }

  Component.current.afterElaboration(build())
}

class LogicAnalyzerTest extends Component {
  val io = new Bundle {
    val x = in UInt(8 bits)
  }

  val xInput = io.x
  val buf = RegNext(io.x)

  val jtag = Jtag()
  jtag.tck := False
  jtag.tdi := False
  jtag.tms := False

  val logicAnalyzer = new LogicAnalyzer(LogicAnalyzerConfig(32), jtag) {
    capture(xInput, (y: UInt) => y === 123)
    //capture(xInput, (y: UInt) => y === 111)
  }
}

import spinal.core.sim._

object LogicAnalyzerSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withVcdWave.compile(new LogicAnalyzerTest()).doSim { dut =>
      val clockDomain = dut.clockDomain.get

      val clockThread = fork {
        for (i <- 0 until 1000) {
          clockDomain.clockToggle()
          sleep(1)
        }
      }

      val pokeThread = fork {
        for (i <- 0 until 256) {
          sleep(1)
          dut.io.x #= 123
          sleep(1)
        }
      }

      pokeThread.join()
      clockThread.join()
    }
  }
}
