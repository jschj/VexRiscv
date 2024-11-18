package mimasa7

import spinal.core._
import spinal.lib._


class Apb3Probe() {

}


case class IntegratedLogicAnalyzerConfig()

class IntegratedLogicAnalyzer(config: IntegratedLogicAnalyzerConfig) extends Component {
  /*
  val io = new Bundle {



    val ledRunning = out Bool()
  }

  val ledRunningLogic = new Area {
    val CYCLES_PER_SECOND = ClockDomain.current.frequency.getValue.v.toInt
    val ledState = Reg(Bool()) init False
    val counter = Reg(UInt(32 bits)) init 0

    when (counter === CYCLES_PER_SECOND - 1) {
      counter := 0
      ledState := !ledState
    } otherwise {
      counter := counter + 1
    }
  }

  io.ledRunning := ledRunningLogic.ledState
   */
}
