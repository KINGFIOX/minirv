/** @brief
  *   这个只是用来做跳转的, 获取可以用来训练分支预测器
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object BRUOpType extends ChiselEnum {
  val bru_X, bru_BLT, bru_BLTU, bru_BGE, bru_BGEU, bru_BEQ, bru_BNE = Value
}

class BRU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val a       = Input(UInt(XLEN.W))
    val b       = Input(UInt(XLEN.W))
    val bru_op  = Input(BRUOpType())
    val br_flag = Output(Bool())
  })

  io.br_flag := false.B // default

  switch(io.bru_op) {
    is(BRUOpType.bru_BLT) {
      when(io.a.asSInt < io.b.asSInt) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BLTU) {
      when(io.a < io.b) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BGE) {
      when(io.a.asSInt >= io.b.asSInt) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BGEU) {
      when(io.a >= io.b) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BEQ) {
      when(io.a === io.b) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BNE) {
      when(io.a =/= io.b) {
        io.br_flag := true.B
      }
    }

  }
}
