package io.trace

import chisel3._
import chisel3.util._

/** @brief
  *   这些就不用参数化了, 已经写死了
  */
class DRAM extends BlackBox {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val a   = Input(UInt(16.W))
    val we  = Input(UInt(4.W))
    val d   = Input(UInt(32.W))
    val spo = Output(UInt(32.W))
  })
}

/** @brief
  */
class IROM extends BlackBox {
  val io = IO(new Bundle {
    val a   = Input(UInt(16.W))
    val spo = Output(UInt(32.W))
  })
}
