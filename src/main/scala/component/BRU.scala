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
    val rs1     = Input(UInt(XLEN.W))
    val rs2     = Input(UInt(XLEN.W))
    val bru_op  = Input(BRUOpType())
    val br_flag = Output(Bool())
  })

  io.br_flag := false.B // default

  switch(io.bru_op) {
    is(BRUOpType.bru_BLT) {
      when(io.rs1.asSInt < io.rs2.asSInt) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BLTU) {
      when(io.rs1 < io.rs2) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BGE) {
      when(io.rs1.asSInt >= io.rs2.asSInt) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BGEU) {
      when(io.rs1 >= io.rs2) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BEQ) {
      when(io.rs1 === io.rs2) {
        io.br_flag := true.B
      }
    }
    is(BRUOpType.bru_BNE) {
      when(io.rs1 =/= io.rs2) {
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
