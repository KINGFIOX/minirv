package hitsz

import chisel3._
import hitsz.io.miniRV_SoC

/** @brief
  */
object Main {
  private def emitVerilog(path: String, mod: => RawModule) = {
    val code = circt.stage.ChiselStage.emitSystemVerilog(
      mod
      // firtoolOpts = Array()
    )
    import java.io._
    val writer = new PrintWriter(new File(path))
    writer.write(code)
    writer.close()
  }

  def main(args: Array[String]): Unit = {
    emitVerilog("generated/miniRV_SoC.v", new miniRV_SoC)
    // emitVerilog("vivado/proj_pipeline.srcs/sources_1/new/SoC.v", new SoC)
  }
}
