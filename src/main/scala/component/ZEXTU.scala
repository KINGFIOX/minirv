package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object ZEXTUOpType extends ChiselEnum {
  val zextu_X, zextu_CSR = Value
}

class ZEXTU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    val op   = Input(ZEXTUOpType())
    val ext  = Output(UInt(XLEN.W))
  })

  io.ext := 0.U // default

  switch(io.op) {
    is(ZEXTUOpType.zextu_X) {
      io.ext := 0.U
    }
    is(ZEXTUOpType.zextu_CSR) {
      val imm = io.inst(19, 15) // 5
      io.ext := Cat(Fill(27 /* 32 - 5 */, 0.U), imm)
    }
  }
}
