package hitsz.io.blackbox

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
