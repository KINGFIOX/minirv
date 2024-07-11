package component;

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import upickle.default

object ALUOpType extends ChiselEnum {
  val alu_X, alu_ADD, alu_SUB, alu_AND, alu_OR, alu_XOR, alu_SLL, alu_SRL, alu_SRA, alu_SLT, alu_SLTU, alu_BLT, alu_BLTU, alu_BGE, alu_BGEU, alu_BEQ, alu_BNE, alu_JALR = Value
}

class ALU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val a       = Input(UInt(XLEN.W))
    val b       = Input(UInt(XLEN.W)) // 这个可能是: 立即数, 寄存器值, 或者是 PC
    val alu_op  = Input(ALUOpType())
    val out     = Output(UInt(XLEN.W))
    val br_flag = Output(Bool())
  })

  io.out     := 0.U // default
  io.br_flag := false.B // 默认不跳转

  // 定义 ALU 的操作
  switch(io.alu_op) {
    is(ALUOpType.alu_X) {
      io.out := 0.U // ALU 啥也不干
    }
    is(ALUOpType.alu_ADD) {
      io.out := io.a + io.b
    }
    is(ALUOpType.alu_SUB) {
      io.out := io.a - io.b
    }
    is(ALUOpType.alu_AND) {
      io.out := io.a & io.b
    }
    is(ALUOpType.alu_OR) {
      io.out := io.a | io.b
    }
    is(ALUOpType.alu_XOR) {
      io.out := io.a ^ io.b
    }
    is(ALUOpType.alu_SLL) {
      io.out := io.a << io.b(log2Up(XLEN) - 1, 0) // 假设最多移位 31 位
    }
    is(ALUOpType.alu_SRL) {
      io.out := io.a >> io.b(log2Up(XLEN) - 1, 0)
    }
    is(ALUOpType.alu_SRA) {
      io.out := (io.a.asSInt >> io.b(log2Up(XLEN) - 1, 0)).asUInt
    }
    is(ALUOpType.alu_SLT) {
      io.out := (io.a.asSInt < io.b.asSInt).asUInt
    }
    is(ALUOpType.alu_SLTU) {
      io.out := (io.a < io.b).asUInt
    }
    is(ALUOpType.alu_BLT) {
      when(io.a.asSInt < io.b.asSInt) {
        io.br_flag := true.B
      }
    }
    is(ALUOpType.alu_BLTU) {
      when(io.a < io.b) {
        io.br_flag := true.B
      }
    }
    is(ALUOpType.alu_BGE) {
      when(io.a.asSInt >= io.b.asSInt) {
        io.br_flag := true.B
      }
    }
    is(ALUOpType.alu_BGEU) {
      when(io.a >= io.b) {
        io.br_flag := true.B
      }
    }
    is(ALUOpType.alu_BEQ) {
      when(io.a === io.b) {
        io.br_flag := true.B
      }
    }
    is(ALUOpType.alu_BNE) {
      when(io.a =/= io.b) {
        io.br_flag := true.B
      }
    }
    is(ALUOpType.alu_JALR) {
      io.out := (io.a + io.b) & ~1.U(XLEN.W)
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
