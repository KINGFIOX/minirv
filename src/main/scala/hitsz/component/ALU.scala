/** @brief
  *   ALU
  */

package hitsz.component;

import chisel3._
import chisel3.util._
import upickle.default

import hitsz.common.HasCoreParameter

/** @brief
  *   ALU 不用管 jal 和 br 的操作, 这个是 NPC 和 BRU 的事情
  * @br
  *   pc += sext(offset)
  */
object ALUOpType extends ChiselEnum {
  val alu_X, alu_ADD, alu_SUB, alu_AND, alu_OR, alu_XOR, alu_SLL, alu_SRL, alu_SRA, alu_SLT, alu_SLTU = Value
}

class ALU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val op1_v  = Input(UInt(XLEN.W))
    val op2_v  = Input(UInt(XLEN.W)) // 这个可能是: 立即数, 寄存器值
    val alu_op = Input(ALUOpType())
    val out    = Output(UInt(XLEN.W))
  })

  io.out := 0.U // default

  // 定义 ALU 的操作
  switch(io.alu_op) {
    is(ALUOpType.alu_X) {
      io.out := 0.U // ALU 啥也不干
    }
    is(ALUOpType.alu_ADD) {
      io.out := (io.op1_v.asSInt + io.op2_v.asSInt).asUInt
    }
    is(ALUOpType.alu_SUB) {
      io.out := (io.op1_v.asSInt - io.op2_v.asSInt).asUInt
    }
    is(ALUOpType.alu_AND) {
      io.out := io.op1_v & io.op2_v
    }
    is(ALUOpType.alu_OR) {
      io.out := io.op1_v | io.op2_v
    }
    is(ALUOpType.alu_XOR) {
      io.out := io.op1_v ^ io.op2_v
    }
    is(ALUOpType.alu_SLL) {
      io.out := io.op1_v << io.op2_v(log2Up(XLEN) - 1, 0) // 假设最多移位 31 位
    }
    is(ALUOpType.alu_SRL) {
      io.out := io.op1_v >> io.op2_v(log2Up(XLEN) - 1, 0)
    }
    is(ALUOpType.alu_SRA) {
      io.out := (io.op1_v.asSInt >> io.op2_v(log2Up(XLEN) - 1, 0)).asUInt
    }
    is(ALUOpType.alu_SLT) {
      io.out := (io.op1_v.asSInt < io.op2_v.asSInt).asUInt
    }
    is(ALUOpType.alu_SLTU) {
      io.out := io.op1_v < io.op2_v
    }
  }
}

object ALU extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new ALU,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
