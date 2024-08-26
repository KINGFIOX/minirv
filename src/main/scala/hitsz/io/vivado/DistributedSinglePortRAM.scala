package hitsz.io.vivado

import chisel3._
import chisel3.util._

class DistributedSinglePortRAM(depth: Int, data_width: Int) extends BlackBox {
  val io = IO(new Bundle {
    val a   = Input(UInt(log2Ceil(depth).W)) // address
    val d   = Input(UInt(data_width.W)) // write data
    val clk = Input(Clock())
    val we  = Input(Bool()) // write enable
    val spo = Output(UInt(data_width.W)) // read data
  })
}
