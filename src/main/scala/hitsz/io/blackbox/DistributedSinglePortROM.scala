package hitsz.io.blackbox

import chisel3._
import chisel3.util._

class DistributedSinglePortROM(depth: Int, data_width: Int) extends BlackBox {
  val io = IO(new Bundle {
    val a   = Input(UInt(log2Ceil(depth).W)) // addr
    val spo = Output(UInt(data_width.W)) // data
  })
}
