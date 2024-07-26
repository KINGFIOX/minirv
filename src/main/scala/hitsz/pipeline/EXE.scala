package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.component.ALU
import hitsz.component.BRU
import hitsz.common.HasCoreParameter
import hitsz.component.ALUOpType
import hitsz.component.ALU_op1_sel
import hitsz.component.ALU_op2_sel
import hitsz.component.NPCOpType
import hitsz.component.BRUOpType
import hitsz.component.ALUOPBundle

class EXE extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val alu_ctrl = Flipped(new ALUOPBundle)
    val alu_out  = Output(UInt(XLEN.W))
    val branch /* 包含有 jal, jalr */ = new Bundle {
      val npc_op = Input(NPCOpType())
      val bru_op = Input(BRUOpType())
    }
    val imm = Input(UInt(XLEN.W))
    val rf_read = new Bundle {
      val rs1_v = Input(UInt(XLEN.W))
      val rs2_v = Input(UInt(XLEN.W))
    }
    val pc = Input(UInt(XLEN.W))
    val if_ = new Bundle {
      val npc_op  = Output(NPCOpType())
      val imm     = Output(UInt(XLEN.W))
      val rs1_v   = Output(UInt(XLEN.W))
      val br_flag = Output(Bool())
    }
  })

  private val alu_ = Module(new ALU)
  alu_.io.alu_op := io.alu_ctrl.calc
  alu_.io.op1_v := Mux1H(
    Seq(
      (io.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_ZERO) -> 0.U,
      (io.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_RS1)  -> io.rf_read.rs1_v,
      (io.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_PC)   -> (io.pc)
    )
  )
  alu_.io.op2_v := Mux1H(
    Seq(
      (io.alu_ctrl.op2_sel === ALU_op2_sel.alu_op2sel_ZERO) -> 0.U,
      (io.alu_ctrl.op2_sel === ALU_op2_sel.alu_op2sel_IMM)  -> io.imm,
      (io.alu_ctrl.op2_sel === ALU_op2_sel.alu_op2sel_RS2)  -> io.rf_read.rs2_v
    )
  )
  io.alu_out := alu_.io.out

  io.if_.npc_op := io.branch.npc_op // default: npc_4

  // beq rs1, rs2, offset => if(rs1==rs2) pc=pc+offset
  private val bru_ = Module(new BRU)
  bru_.io.in.rs1_v  := io.rf_read.rs1_v
  bru_.io.in.rs2_v  := io.rf_read.rs2_v
  bru_.io.in.bru_op := io.branch.bru_op
  io.if_.br_flag    := bru_.io.br_flag // 是否跳转

  // jalr rd, offset(rs1) => t=pc+4; pc=(rs1_v+offset)&~1; rd_v=t
  // jal rd, offset => rd=pc+4; pc=pc+offset
  io.if_.imm   := io.imm //
  io.if_.rs1_v := io.rf_read.rs1_v // 只有 jalr 会用

}

object EXE extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new EXE,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
