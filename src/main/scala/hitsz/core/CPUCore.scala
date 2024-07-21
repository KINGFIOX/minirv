package hitsz.core

import chisel3._
import chisel3.util._

import hitsz.component.IF
import hitsz.common.HasCoreParameter
import hitsz.component.RegFile
import hitsz.component.CU
import hitsz.component.ALU
import hitsz.component.MemU
import hitsz.component.ALU_op1_sel
import hitsz.component.ALU_op2_sel
import hitsz.component.WB_sel
import hitsz.component.BRU

import hitsz.io.blackbox.InstROMBundle
import hitsz.io.DebugBundle
import hitsz.component.CSR_op1_sel
import hitsz.component.IntPollU
import hitsz.component.Bus_Arbit
import hitsz.component.CSRALU
import hitsz.component.CSRRegFile
import hitsz.component.CSRWbStage
import hitsz.component.HasCSRConst
import hitsz.component.MCAUSE_CONSTS
import hitsz.utils.ZeroExt

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

class CPUCore extends Module with HasCoreParameter with HasCSRConst {
  val io = IO(new Bundle {
    val irom  = Flipped(new InstROMBundle)
    val bus   = new BusBundle
    val debug = new DebugBundle
  })

  /* ---------- IF ---------- */

  // ***** CSR Reg *****
  val csr_rf_ = new CSRRegFile

  // ***** IF U *****

  val if_ = Module(new IF) // Instruction Fetch: NPC + PC
  io.irom <> if_.io.irom
  if_.io.csr.mcause := csr_rf_.read(MCAUSE.U)
  if_.io.csr.mepc   := csr_rf_.read(MEPC.U)

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

  // ***** CSR ALU *****

  val csru_alu_ = Module(new CSRALU)
  csru_alu_.io.calc  := cu_.io.ctrl.csr_alu.calc
  csru_alu_.io.csr_v := csr_rf_.read(cu_.io.ctrl.csr_alu.csr_i)
  csru_alu_.io.op1 := Mux1H(
    Seq(
      (cu_.io.ctrl.csr_alu.op1_sel === CSR_op1_sel.csr_op1_X)    -> 0.U,
      (cu_.io.ctrl.csr_alu.op1_sel === CSR_op1_sel.csr_op1_ZIMM) -> cu_.io.imm,
      (cu_.io.ctrl.csr_alu.op1_sel === CSR_op1_sel.csr_op1_RS1)  -> regfile_.read(cu_.io.rf.rs1_i)
    )
  )

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
  intpollu_.io.en        := false.B

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
      when(csr_rf_.read(MSTATUS.U) =/= 0.U) { // 中断使能
        io.bus.addr     := intpollu_.io.bus.addr
        intpollu_.io.en := true.B
      }
    }
    is(Bus_Arbit.bus_MEM) {
      io.bus.addr  := mem_.io.bus.addr
      io.bus.wen   := mem_.io.bus.wen
      io.bus.wdata := mem_.io.bus.wdata
    }
  }

  /* ---------- WB ---------- */

  /// 指令的生效就是在 wb 阶段, 如果在 wb 发现了 IntPollU 输出有效。
  /// 说明：是在这条指令的 mem 阶段的时候, 产生了中断, 这个时候, 认为这条指令没有执行完成, 需要重新执行当前指令

  // ***** reg file *****

  csr_rf_.write(MCAUSE.U, 0.U) // mcause 大多数情况下是 0

  when(intpollu_.io.valid) { // 中断
    regfile_.write(MEPC.U, pc_4 - 4.U)
    csr_rf_.write(MCAUSE.U, ZeroExt(MCAUSE_CONSTS.INT))
  }.otherwise {
    switch(cu_.io.ctrl.wb_sel) {
      is(WB_sel.wbsel_X) { /* 啥也不干 */ }
      is(WB_sel.wbsel_ALU) { regfile_.write(cu_.io.rf.rd_i, alu_.io.out) }
      is(WB_sel.wbsel_CSR) { regfile_.write(cu_.io.rf.rd_i, csru_alu_.io.orig) }
      is(WB_sel.wbsel_MEM) { regfile_.write(cu_.io.rf.rd_i, mem_.io.rdata) }
      is(WB_sel.wbsel_PC4) { regfile_.write(cu_.io.rf.rd_i, if_.io.out.pc_4) }
    }
    // ***** csr reg file *****
    switch(cu_.io.ctrl.csr_wb_stag) {
      // TODO 例外: 注意, 这里可能会出现非法访问的现象
      is(CSRWbStage.csr_wb_X) { /* 啥也不干 */ }
      is(CSRWbStage.csr_wb_ALU) { csr_rf_.write(cu_.io.ctrl.csr_alu.csr_i, csru_alu_.io.after) }
      is(CSRWbStage.csr_wb_ECALL) {
        csr_rf_.write(MEPC.U, if_.io.out.pc_4) // mepc 下一条指令的地址
        csr_rf_.write(MCAUSE.U, MCAUSE_CONSTS.ECALL.U) //
      }
    }
  }

  /* ---------- debug ---------- */

  // 这个 debug 的线，正好差了一个周期，懒得整了，直接 RegNext

  io.debug.wb_have_inst := RegNext(true.B)
  io.debug.wb_pc        := RegNext(if_.io.out.pc_4 - 4.U)
  io.debug.wb_ena       := RegNext(Mux(cu_.io.ctrl.wb_sel =/= WB_sel.wbsel_X, true.B, false.B))
  io.debug.wb_reg       := RegNext(cu_.io.rf.rd_i)
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
