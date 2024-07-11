package component;

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import upickle.default

/** @brief
  *   ALU 不用管 jal 和 br 的操作, 这个是 NPC 和 BRU 的事情
  * @br
  *   pc += sext(offset)
  * @jal
  *   pc += sext(offset)
  * @jalr
  *   pc = ra1 + sext(offset) & ~1
  */
object ALUOpType extends ChiselEnum {
  val alu_X, alu_ADD, alu_SUB, alu_AND, alu_OR, alu_XOR, alu_SLL, alu_SRL, alu_SRA, alu_SLT, alu_SLTU, alu_JALR = Value
}

class ALU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val rs1    = Input(UInt(XLEN.W))
    val rs2    = Input(UInt(XLEN.W)) // 这个可能是: 立即数, 寄存器值
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
      io.out := io.rs1 + io.rs2
    }
    is(ALUOpType.alu_SUB) {
      io.out := io.rs1 - io.rs2
    }
    is(ALUOpType.alu_AND) {
      io.out := io.rs1 & io.rs2
    }
    is(ALUOpType.alu_OR) {
      io.out := io.rs1 | io.rs2
    }
    is(ALUOpType.alu_XOR) {
      io.out := io.rs1 ^ io.rs2
    }
    is(ALUOpType.alu_SLL) {
      io.out := io.rs1 << io.rs2(log2Up(XLEN) - 1, 0) // 假设最多移位 31 位
    }
    is(ALUOpType.alu_SRL) {
      io.out := io.rs1 >> io.rs2(log2Up(XLEN) - 1, 0)
    }
    is(ALUOpType.alu_SRA) {
      io.out := (io.rs1.asSInt >> io.rs2(log2Up(XLEN) - 1, 0)).asUInt
    }
    is(ALUOpType.alu_SLT) {
      io.out := (io.rs1.asSInt < io.rs2.asSInt).asUInt
    }
    is(ALUOpType.alu_SLTU) {
      io.out := (io.rs1 < io.rs2).asUInt
    }
    is(ALUOpType.alu_JALR) {
      // rs1
      // rs2 = sext(offset)
      io.out := (io.rs1 + io.rs2) & ~1.U(XLEN.W)
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
