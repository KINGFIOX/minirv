package component;

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import upickle.default

object ALUOpType extends ChiselEnum {
  val alu_ADD, alu_SUB, alu_AND, alu_OR, alu_XOR, alu_SLL, alu_SRL, alu_SRA, alu_LT, alu_LTU, alu_GE, alu_GEU, alu_EQ, alu_NE = Value
}

class ALU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val a      = Input(UInt(XLEN.W))
    val b      = Input(UInt(XLEN.W))
    val alu_op = Input(ALUOpType())
    val out    = Output(UInt(XLEN.W))
  })

  io.out := 0.U // default

  // 定义 ALU 的操作
  switch(io.alu_op) {
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
    is(ALUOpType.alu_LT) {
      io.out := (io.a.asSInt < io.b.asSInt).asUInt
    }
    is(ALUOpType.alu_LTU) {
      io.out := (io.a < io.b).asUInt
    }
    is(ALUOpType.alu_GE) {
      io.out := (io.a.asSInt >= io.b.asSInt).asUInt
    }
    is(ALUOpType.alu_GEU) {
      io.out := (io.a >= io.b).asUInt
    }
    is(ALUOpType.alu_EQ) {
      io.out := (io.a === io.b).asUInt
    }
    is(ALUOpType.alu_NE) {
      io.out := (io.a =/= io.b).asUInt
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
