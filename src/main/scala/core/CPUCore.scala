package core

import chisel3._
import chisel3.util._

import component.IF
import common.HasCoreParameter
import component.RegFile
import component.CU
import component.ALU
import component.MemU
import component.ALU_op1_sel
import component.ALU_op2_sel
import component.WB_sel
import component.BRU

import io.blackbox.InstROMBundle
import io.DebugBundle
import component.CSRU
import component.CSR_op1_sel
import component.IntPollU
import component.Bus_Arbit

/** @brief
  *   这个是站在 CPU 视角的, 总线, 与 DRAM 交互的
  */
class BusBundle extends Bundle with HasCoreParameter {
  val addr  = Output(UInt(XLEN.W))
  val rdata = Input(UInt(XLEN.W))
  val wen   = Output(UInt(dataBytes.W))
  val wdata = Output(UInt(XLEN.W))
}

// 命名约束: 有下划线的是模块

class CPUCore extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val irom = Flipped(new InstROMBundle)
    val bus  = new BusBundle
    // val int_  = new IntBundle
    val debug = new DebugBundle
  })

  /* ---------- IF ---------- */

  val if_ = Module(new IF) // Instruction Fetch: NPC + PC
  io.irom <> if_.io.irom

  private val cur_inst = if_.io.out.inst
  private val pc_4     = if_.io.out.pc_4

  /* ---------- ID ---------- */

  val cu_ = Module(new CU)
  cu_.io.inst := cur_inst
  // cu_.io.int_ := io.int_

  val regfile_ = new RegFile

  /* ---------- EXE ---------- */

  // ***** ALU *****

  val alu_ = Module(new ALU)
  alu_.io.alu_op := cu_.io.ctrl.alu.calc
  alu_.io.op1_v := Mux1H(
    Seq(
      (cu_.io.ctrl.alu.op1 === ALU_op1_sel.alu_op1sel_ZERO) -> 0.U,
      (cu_.io.ctrl.alu.op1 === ALU_op1_sel.alu_op1sel_RS1)  -> regfile_.read(cu_.io.rf.rs1_i),
      (cu_.io.ctrl.alu.op1 === ALU_op1_sel.alu_op1sel_PC)   -> (if_.io.out.pc_4 - 4.U)
    )
  )
  alu_.io.op2_v := Mux1H(
    Seq(
      (cu_.io.ctrl.alu.op2 === ALU_op2_sel.alu_op2sel_ZERO) -> 0.U,
      (cu_.io.ctrl.alu.op2 === ALU_op2_sel.alu_op2sel_IMM)  -> cu_.io.imm,
      (cu_.io.ctrl.alu.op2 === ALU_op2_sel.alu_op2sel_RS2)  -> regfile_.read(cu_.io.rf.rs2_i)
    )
  )

  // ***** CSR *****

  val csru_ = Module(new CSRU)
  csru_.io.calc  := cu_.io.ctrl.csr.calc
  csru_.io.csr_i := cu_.io.ctrl.csr.csr_i
  csru_.io.op1 := Mux1H(
    Seq(
      (cu_.io.ctrl.csr.op1_sel === CSR_op1_sel.csr_op1_X)    -> 0.U,
      (cu_.io.ctrl.csr.op1_sel === CSR_op1_sel.csr_op1_ZIMM) -> cu_.io.imm,
      (cu_.io.ctrl.csr.op1_sel === CSR_op1_sel.csr_op1_RS1)  -> regfile_.read(cu_.io.rf.rs1_i)
    )
  )

  // default
  csru_.io.pc_4  := if_.io.out.pc_4
  if_.io.in.mepc := csru_.io.out

  // ***** BRU *****

  if_.io.in.op := cu_.io.ctrl.npc_op // default: npc_4

  // beq rs1, rs2, offset => if(rs1==rs2) pc=pc+offset
  val bru_ = Module(new BRU)
  bru_.io.in.rs1_v  := regfile_.read(cu_.io.rf.rs1_i)
  bru_.io.in.rs2_v  := regfile_.read(cu_.io.rf.rs2_i)
  bru_.io.in.bru_op := cu_.io.ctrl.bru_op
  if_.io.in.br_flag := bru_.io.br_flag // 是否跳转

  // jalr rd, offset(rs1) => t=pc+4; pc=(rs1_v+offset)&~1; rd_v=t
  // jal rd, offset => rd=pc+4; pc=pc+offset
  if_.io.in.imm   := cu_.io.imm //
  if_.io.in.rs1_v := regfile_.read(cu_.io.rf.rs1_i) // 只有 jalr 会用

  /* ---------- MEM ---------- */

  // ***** interrupt poll *****
  val intpollu_ = Module(new IntPollU)
  intpollu_.io.bus.rdata := io.bus.rdata

  // ***** mem *****
  val mem_ = Module(new MemU)
  mem_.io.in.op     := cu_.io.ctrl.op_mem
  mem_.io.in.addr   := alu_.io.out
  mem_.io.in.wdata  := regfile_.read(cu_.io.rf.rs2_i)
  mem_.io.bus.rdata := io.bus.rdata

  // ----- bus -----

  // default
  io.bus.addr  := 0.U
  io.bus.wen   := 0.U
  io.bus.wdata := 0.U

  switch(cu_.io.ctrl.bus_arbit) {
    is(Bus_Arbit.bus_X) { /* 啥也不干 */ }
    is(Bus_Arbit.bus_IPoll) {
      io.bus.addr := intpollu_.io.bus.addr
    }
    is(Bus_Arbit.bus_MEM) {
      io.bus.addr  := mem_.io.bus.addr
      io.bus.wen   := mem_.io.bus.wen
      io.bus.wdata := mem_.io.bus.wdata
    }
  }

  /* ---------- WB ---------- */

  switch(cu_.io.ctrl.wb_sel) {
    is(WB_sel.wbsel_X) { /* 啥也不干 */ }
    is(WB_sel.wbsel_ALU) {
      regfile_.write(cu_.io.rf.rd_i, alu_.io.out)
    }
    is(WB_sel.wbsel_CSR) {
      regfile_.write(cu_.io.rf.rd_i, csru_.io.out)
    }
    is(WB_sel.wbsel_MEM) {
      regfile_.write(cu_.io.rf.rd_i, mem_.io.rdata)
    }
    is(WB_sel.wbsel_PC4) {
      regfile_.write(cu_.io.rf.rd_i, if_.io.out.pc_4)
    }
  }

  /* ---------- debug ---------- */

  // 这个 debug 的线，正好差了一个周期，懒得整了，直接 RegNext

  io.debug.wb_have_inst := RegNext(true.B)
  io.debug.wb_pc        := RegNext(if_.io.out.pc_4 - 4.U)
  // printf("wb_pc=%x\n", io.debug.wb_pc)
  // printf("pc_4=%x\n", if_.io.out.pc_4)
  io.debug.wb_ena := RegNext(Mux(cu_.io.ctrl.wb_sel =/= WB_sel.wbsel_X, true.B, false.B))
  io.debug.wb_reg := RegNext(cu_.io.rf.rd_i)
  // printf("wb_reg=%x\n", io.debug.wb_reg)
  // printf("wb_value=%x\n", io.debug.wb_value)
  io.debug.wb_value := RegNext(
    MuxCase(
      0.U,
      Seq(
        (cu_.io.ctrl.wb_sel === WB_sel.wbsel_ALU) -> alu_.io.out,
        (cu_.io.ctrl.wb_sel === WB_sel.wbsel_MEM) -> mem_.io.rdata,
        (cu_.io.ctrl.wb_sel === WB_sel.wbsel_PC4) -> if_.io.out.pc_4
      )
    )
  )
  // val rd = cu_.io.rf.rd_i
}

object CPUCore extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new CPUCore,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
