package mimasa7

import spinal.core._
import spinal.core.sim.SimConfig
import spinal.lib._
import spinal.lib.com.jtag._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.com.jtag.sim.{JtagDriver, JtagTcp, JtagVpi}
import vexriscv.demo.Murax

import java.io.{InputStream, OutputStream}
import java.net.ServerSocket
import spinal.core.sim._
import spinal.lib.com.jtag.Jtag
import spinal.sim.SimManagerContext


object MyJtagTcp {
  def apply(jtag: Jtag, jtagClkPeriod: Long) = fork {
    var inputStream: InputStream = null
    var outputStream: OutputStream = null

    class SocketThread extends Thread  {
      val socket = new ServerSocket(7894)
      override def run() : Unit = {
        println("WAITING FOR TCP JTAG CONNECTION")
        while (true) {
          val connection = try { socket.accept() } catch { case e : Exception => return }
          connection.setTcpNoDelay(true)
          outputStream = connection.getOutputStream()
          inputStream = connection.getInputStream()
          println("TCP JTAG CONNECTION")
        }
      }
    }
    val server = new SocketThread
    onSimEnd (server.socket.close())
    server.start()

    while (true) {
      sleep(jtagClkPeriod * 200)

      while (inputStream != null && inputStream.available() != 0) {
        val buffer = inputStream.read()

        //println(s"Read ${buffer.toHexString}")

        jtag.tms #= (buffer & 1) != 0;
        jtag.tdi #= (buffer & 2) != 0;
        jtag.tck #= (buffer & 8) != 0;
        if ((buffer & 4) != 0) {
          outputStream.write(if (jtag.tdo.toBoolean) 1 else 0)
        }
        sleep(jtagClkPeriod / 2)
      }
    }
  }
}

class JtagDriverWrapper(_jtag: Jtag, _clockPeriod: TimeNumber) extends JtagDriver(_jtag, _clockPeriod) {
  private val instructionWidth = 8
  private val drWidth = 32

  private def serialize(value: Int, bitCount: Int): Seq[Boolean] = {
    require(bitCount <= 32)

    (0 until bitCount).map { i =>
      // TODO: Endianness?
      val bitMask = 1 << i
      (value & bitMask) != 0
    }
  }

  private def deserialize(seq: Seq[Boolean], bitCount: Int): Int = {
    require(seq.length >= bitCount)

    (0 until bitCount).map { i =>
      // TODO: Endianness?
      val shiftAmount = i
      val bitValue = if (seq(i)) 1 else 0
      bitValue << shiftAmount
    }.fold(0)((a, b) => a | b)
  }

  def doReadDR(instructionId: Int): Int = {
    doResetTap()

    // set instruction register
    // assume RESET state, then go into SHIFT IR (01100)
    doTmsSeq(Seq(false, true, true, false, false))
    // actually write instruction
    val seq = serialize(instructionId, instructionWidth)
    println(s"$instructionId -> seq=$seq")
    doScanChain(seq, true)

    // go from EXIT IR into UPDATE IR (1)
    doTmsSeq(Seq(true))

    // go into IDLE (0)
    doTmsSeq(Seq(false))

    // assume IDLE state, then go into SHIFT DR (0100)
    doTmsSeq(Seq(true, false, false))

    // read DR
    val scannedData = doScanChain(Seq.fill(32)(false), false)
    deserialize(scannedData, drWidth)
  }
}

class MimasA7JtagTap extends Component {
  val io = new Bundle {
    val jtag    = slave(Jtag())
    //val switchs = in  Bits(8 bit)
    //val keys    = in  Bits(4 bit)
    val leds    = out Bits(8 bit)
    // debug output
    val internalState = out UInt(8 bits)
  }

  val ctrl = new ClockingArea(ClockDomain(io.jtag.tck)) {
    val ledsBuf = Reg(Bits(8 bits)) //init B"00000000"
    val magicReg = Reg(UInt(32 bits))
    magicReg := U(B"xABABCDCD")

    val tap = new JtagTap(io.jtag, 8)
    val idcodeArea0 = tap.idcode(B"x87654321")(instructionId = 4)
    //val idcodeArea1 = tap.idcode(B"xFEDCBA98")(instructionId = 1)
    //val magicArea = tap.read(magicReg)(instructionId = 5)
    //val switchsArea = tap.read(io.switchs)(instructionId = 5)
    //val keysArea = tap.read(io.keys)(instructionId = 6)
    val ledsArea = tap.write(ledsBuf)(instructionId = 7)
    io.leds := ledsBuf

    io.internalState := tap.fsm.state.asBits.asUInt.resized
  }
}

object MimasA7JtagTap {
  def apply(): MimasA7JtagTap = new MimasA7JtagTap()

  def main(args: Array[String]): Unit = {
    val spinalConfig = SpinalConfig()
    SpinalVerilog(spinalConfig)(MimasA7JtagTap())
  }
}

object MimasA7JtagTapSim {
  def main(args: Array[String]): Unit = {
    SimConfig.compile(MimasA7JtagTap()).doSimUntilVoid { dut =>
      val jtagClockPeriod = 1
      val period = TimeNumber(SimManagerContext.current.manager.timePrecision * 2)
      val clockDomain = ClockDomain(dut.io.jtag.tck)
      //dut.clockDomain.forkStimulus(1 Hz)
      //clockDomain.forkStimulus(1 Hz)

      //val clockPeriod = TimeNumber(2)
      //val value = timeToLong(clockPeriod / 2)
      //println(s"precision = ${SimManagerContext.current.manager.timePrecision}")

      //val jtagTcpThread = MyJtagTcp(dut.io.jtag, jtagClockPeriod)
      val jtagTcpThread = JtagVpi(dut.io.jtag, port = 5555, jtagClkPeriod = period)
      //val jtagDriver = new JtagDriverWrapper(dut.io.jtag, period)

      val clockThread = fork {
        while (true) {
          //println("clock loop")
          clockDomain.clockToggle()
          sleep(jtagClockPeriod)
        }
      }

      val stateThread = fork {
        var lastState = -1
        var lastLeds = -1

        def stateName(state: Int): String =
          if (state < 0 || state >= JtagState.elements.length)
            "<invalid state>"
          else
            s"${JtagState.elements(state).name} ($state)"

        while (true) {
          val currentState = dut.io.internalState.toInt

          if (currentState != lastState)
            println(s"State transition: ${stateName(lastState)} -> ${stateName(currentState)}")

          lastState = currentState


          val currentLeds = dut.io.leds.toInt

          if (currentLeds != lastLeds)
            println(s"LEDs: ${currentLeds.toBinaryString}")

          lastLeds = currentLeds


          // sleep() is a kind of yield()
          sleep(jtagClockPeriod)
        }
      }

      /*
      println("performing reset")
      jtagDriver.doResetTap()
      println("done")

      println("reading IDCODE")
      val idCode = jtagDriver.doReadDR(0)
      println(s"DR = 0x${idCode.toHexString}")
      println("done")

      println("reading DR")
      val dr = jtagDriver.doReadDR(5)
      println(s"DR = 0x${dr.toHexString}")
      println("done")
       */

      stateThread.join()
      clockThread.join()
      jtagTcpThread.join()
    }
  }
}

object Test {
  def main(args: Array[String]): Unit = {
    val states = JtagState()

    for (el <- states.spinalEnum.elements)
      println(s"${el.position} -> ${el.name}")


  }
}