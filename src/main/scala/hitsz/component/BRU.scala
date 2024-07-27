/** @brief
  *   这个只是用来做跳转的, 获取可以用来训练分支预测器
  */

package hitsz.component

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter

object BRUOpType extends ChiselEnum {
  val bru_X, bru_BLT, bru_BLTU, bru_BGE, bru_BGEU, bru_BEQ, bru_BNE = Value
}

class BRU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val in = new Bundle {
      val bru_op = Input(BRUOpType())
      val rs1_v  = Input(UInt(XLEN.W))
      val rs2_v  = Input(UInt(XLEN.W))
    }
    // val br_flag = Output(Bool())
    val br_flag = Output(Bool())
  })

  io.br_flag := false.B // default

  switch(io.in.bru_op) {
    is(BRUOpType.bru_BLT) {
      when(io.in.rs1_v.asSInt < io.in.rs2_v.asSInt) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BLTU) {
      when(io.in.rs1_v < io.in.rs2_v) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BGE) {
      when(io.in.rs1_v.asSInt >= io.in.rs2_v.asSInt) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BGEU) {
      when(io.in.rs1_v >= io.in.rs2_v) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BEQ) {
      when(io.in.rs1_v === io.in.rs2_v) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BNE) {
      when(io.in.rs1_v =/= io.in.rs2_v) {
        io.br_flag := true.B
      }
    }

  }
}

object BRU extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new BRU,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
