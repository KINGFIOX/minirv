package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

class IROM extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val addr = Input(UInt(XLEN.W))
    val inst = Output(UInt(32.W))
  })

  // TODO 输出
}
