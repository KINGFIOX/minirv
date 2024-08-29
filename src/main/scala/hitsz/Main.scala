package hitsz

import chisel3._

import hitsz.io.miniRV_SoC

import java.lang.Enum

/** @brief
  *   .sv 文件要输出到 cdp-tests/mySoC/miniRV_SoC.v
  */

object Main extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new miniRV_SoC(isVivado = true, enableDebug = false),
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
