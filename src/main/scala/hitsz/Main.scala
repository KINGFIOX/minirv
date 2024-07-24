package hitsz

import chisel3._

import hitsz.io.miniRV_SoC

import java.lang.Enum

object ENV {
  // 定义: 是输出到 vivado, 还是 verilator
  val isVivado = false
}

/** @brief
  *   .sv 文件要输出到 cdp-tests/mySoC/miniRV_SoC.v
  */

object Main extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new miniRV_SoC,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
