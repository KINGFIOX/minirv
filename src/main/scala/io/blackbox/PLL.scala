package io.blackbox

import chisel3._
import chisel3.util._

/** @brief
  *   时钟降频转换
  */
class PLL extends BlackBox {
  val io = IO(new Bundle {
    val clk_in1  = Input(Clock())
    val clk_out1 = Output(Clock())
    val locked   = Output(Bool())
  })
}

object CLKGen {
  def apply(clk: Clock): Clock = {
    val clkgen = Module(new PLL)
    clkgen.io.clk_in1 := clk
    (clkgen.io.clk_out1.asBool & clkgen.io.locked).asClock
  }
}

object DigDecoder extends App {

  /** @brief
    *   这里验证一下: 会不会有 io_ 前缀。 实验证明: 并没有
    */
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new Module {
      val pll = Module(new PLL)
      pll.io <> DontCare
    },
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
