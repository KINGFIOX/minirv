package core

import chisel3._
import chisel3.util._
import component.IROM

import _root_.debug.DebugBundle
import component.IF
import common.HasCoreParameter
import component.RegFile
import component.CU
import component.ALU
import component.MemU
import component.OP1_sel
import component.OP2_sel
import component.WB_sel
import component.BRU

class InstROMBundle extends Bundle with HasCoreParameter {
  val addr = Output(UInt(XLEN.W)) // FIXME 对 IROM 传入指令的地址, 但是这个地址可能不是 32bit
  val inst = Input(UInt(XLEN.W)) // IROM 传出指令
}

/** @brief
  *   总线, 与 DRAM 交互的
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
    val inst_rom = new InstROMBundle
    val bus      = new BusBundle
    // val debug    = new DebugBundle
  })

  /* ---------- IF ---------- */

  val if_ = Module(new IF) // Instruction Fetch: NPC + PC
  io.inst_rom <> if_.io.irom

  private val cur_inst = if_.io.out.inst
  private val pc_4     = if_.io.out.pc_4

  /* ---------- ID ---------- */

  val cu_ = Module(new CU)
  cu_.io.inst := cur_inst

  val regfile_ = new RegFile

  /* ---------- EXE ---------- */

  val alu_ = Module(new ALU)
  alu_.io.alu_op := cu_.io.ctrl.alu_op
  alu_.io.op1_v := Mux1H(
    Seq(
      (cu_.io.ctrl.op1_sel === OP1_sel.op1sel_ZERO) -> 0.U,
      (cu_.io.ctrl.op1_sel === OP1_sel.op1sel_RS1)  -> regfile_.read(cu_.io.rf.rs1_i),
      (cu_.io.ctrl.op1_sel === OP1_sel.op1sel_PC)   -> (if_.io.out.pc_4 - 4.U)
    )
  )
  alu_.io.op2_v := Mux1H(
    Seq(
      (cu_.io.ctrl.op2_sel === OP2_sel.op2sel_ZERO) -> 0.U,
      (cu_.io.ctrl.op2_sel === OP2_sel.op2sel_IMM)  -> cu_.io.imm,
      (cu_.io.ctrl.op2_sel === OP2_sel.op2sel_RS2)  -> regfile_.read(cu_.io.rf.rs2_i)
    )
  )

  // 应该还会有 csr 之类的

  // beq rs1, rs2, offset => if(rs1==rs2) pc=pc+offset
  private val bru_ = Module(new BRU)
  bru_.io.in.rs1_v  := regfile_.read(cu_.io.rf.rs1_i)
  bru_.io.in.rs2_v  := regfile_.read(cu_.io.rf.rs2_i)
  bru_.io.in.bru_op := cu_.io.ctrl.bru_op
  if_.io.in.br_flag := bru_.io.br_flag // 是否跳转

  // jalr rd, offset(rs1) => t=pc+4; pc=(rs1_v+offset)&~1; rd_v=t
  // jal rd, offset => rd=pc+4; pc=pc+offset
  if_.io.in.op    := cu_.io.ctrl.npc_op // default: npc_4
  if_.io.in.imm   := cu_.io.imm //
  if_.io.in.rs1_v := regfile_.read(cu_.io.rf.rs1_i) // 只有 jalr 会用

  /* ---------- MEM ---------- */

  val mem_ = Module(new MemU)
  mem_.io.bus <> io.bus
  mem_.io.in.op    := cu_.io.ctrl.op_mem
  mem_.io.in.addr  := alu_.io.out
  mem_.io.in.wdata := regfile_.read(cu_.io.rf.rs2_i)

  /* ---------- WB ---------- */

  switch(cu_.io.ctrl.wb_sel) {
    is(WB_sel.wbsel_X) { /* 啥也不干 */ }
    is(WB_sel.wbsel_ALU) {
      regfile_.write(cu_.io.rf.rd_i, alu_.io.out)
    }
    is(WB_sel.wbsel_CSR) {}
    is(WB_sel.wbsel_MEM) {
      regfile_.write(cu_.io.rf.rd_i, mem_.io.out.rdata)
    }
    is(WB_sel.wbsel_PC4) {
      regfile_.write(cu_.io.rf.rd_i, if_.io.out.pc_4)
    }
  }

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
