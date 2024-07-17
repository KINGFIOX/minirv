import chisel3._

object ENV {
  // 定义: 是输出到 vivado, 还是 verilator
}

/** @brief
  *   .sv 文件要输出到 cdp-tests/mySoC/miniRV_SoC.v
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
    emitVerilog("cdp-tests/mySoC/miniRV_SoC.v", new miniRV_SoC)
    // emitVerilog("vivado/proj_pipeline.srcs/sources_1/new/SoC.v", new SoC)
  }
}
