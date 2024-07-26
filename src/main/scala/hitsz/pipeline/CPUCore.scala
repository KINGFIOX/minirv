package hitsz.pipeline

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
import hitsz.utils.pipe

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
    val dbg  = new DebugBundle
  })

  /* ---------- IF ---------- */

  private val if_ = Module(new IF) // Instruction Fetch: NPC + PC
  io.irom <> if_.io.irom

  private val cur_inst = if_.io.out.inst
  private val pc_4     = if_.io.out.pc_4

  /* ---------- ID ---------- */

  private val cu_ = Module(new CU)
  cu_.io.inst := cur_inst

  private val regfile_ = Module(new RegFile)
  // regfile_.io.inst := cur_inst
  regfile_.io.read.rs1_i := cur_inst(19, 15)
  regfile_.io.read.rs2_i := cur_inst(24, 20)
  regfile_.io.write.rd_i := cur_inst(11, 7)

  private val id2exe_l = ID2EXEBundle(
    cu_.io.ctrl,
    cu_.io.imm,
    cur_inst,
    regfile_.io.read.rs1_v,
    regfile_.io.read.rs2_v,
    if_.io.out.pc_4 - 4.U
  )
  /* ---------- EXE ---------- */
  private val id2exe_r = Wire(new ID2EXEBundle)
  // pipe(id2exe_l, id2exe_r, true.B)
  id2exe_r := id2exe_l

  private val exe_ = Module(new EXE)
  exe_.io.alu := id2exe_r.cu.ctrl.alu

  exe_.io.branch.npc_op := id2exe_r.cu.ctrl.npc_op
  exe_.io.branch.bru_op := id2exe_r.cu.ctrl.bru_op

  exe_.io.imm           := id2exe_r.cu.imm
  exe_.io.rf.read.rs1_v := id2exe_r.rf.read.rs1_v
  exe_.io.rf.read.rs2_v := id2exe_r.rf.read.rs2_v
  exe_.io.pc            := id2exe_r.pc

  if_.io.in := exe_.io.if_

  // // TODO CSR

  // val alu_ = Module(new ALU)
  // alu_.io.alu_op := cu_.io.ctrl.alu.calc
  // alu_.io.op1_v := Mux1H(
  //   Seq(
  //     (cu_.io.ctrl.alu.op1 === ALU_op1_sel.alu_op1sel_ZERO) -> 0.U,
  //     (cu_.io.ctrl.alu.op1 === ALU_op1_sel.alu_op1sel_RS1)  -> regfile_.io.read.rs1_v,
  //     (cu_.io.ctrl.alu.op1 === ALU_op1_sel.alu_op1sel_PC)   -> (if_.io.out.pc_4 - 4.U)
  //   )
  // )
  // alu_.io.op2_v := Mux1H(
  //   Seq(
  //     (cu_.io.ctrl.alu.op2 === ALU_op2_sel.alu_op2sel_ZERO) -> 0.U,
  //     (cu_.io.ctrl.alu.op2 === ALU_op2_sel.alu_op2sel_IMM)  -> cu_.io.imm,
  //     (cu_.io.ctrl.alu.op2 === ALU_op2_sel.alu_op2sel_RS2)  -> regfile_.io.read.rs2_v
  //   )
  // )

  // if_.io.in.op := cu_.io.ctrl.npc_op // default: npc_4

  // // beq rs1, rs2, offset => if(rs1==rs2) pc=pc+offset
  // private val bru_ = Module(new BRU)
  // bru_.io.in.rs1_v  := regfile_.io.read.rs1_v
  // bru_.io.in.rs2_v  := regfile_.io.read.rs2_v
  // bru_.io.in.bru_op := cu_.io.ctrl.bru_op
  // if_.io.in.br_flag := bru_.io.br_flag // 是否跳转

  // // jalr rd, offset(rs1) => t=pc+4; pc=(rs1_v+offset)&~1; rd_v=t
  // // jal rd, offset => rd=pc+4; pc=pc+offset
  // if_.io.in.imm   := cu_.io.imm //
  // if_.io.in.rs1_v := regfile_.io.read.rs1_v // 只有 jalr 会用

  /* ---------- MEM ---------- */

  val mem_ = Module(new MemU)
  mem_.io.bus <> io.bus
  mem_.io.in.op    := cu_.io.ctrl.op_mem
  mem_.io.in.addr  := exe_.io.alu_out
  mem_.io.in.wdata := regfile_.io.read.rs2_v

  /* ---------- WB ---------- */

  regfile_.io.write.wdata := 0.U
  regfile_.io.write.wen   := cu_.io.ctrl.wb_wen
  switch(cu_.io.ctrl.wb_sel) {
    is(WB_sel.wbsel_X) { /* 啥也不干 */ }
    is(WB_sel.wbsel_ALU) {
      // regfile_.write(cu_.io.rf.rd_i, alu_.io.out)
      regfile_.io.write.wdata := exe_.io.alu_out
    }
    is(WB_sel.wbsel_CSR) { /* TODO */ }
    is(WB_sel.wbsel_MEM) {
      // regfile_.write(cu_.io.rf.rd_i, mem_.io.rdata)
      regfile_.io.write.wdata := mem_.io.rdata
    }
    is(WB_sel.wbsel_PC4) {
      // regfile_.write(cu_.io.rf.rd_i, if_.io.out.pc_4)
      regfile_.io.write.wdata := if_.io.out.pc_4
    }
  }

  /* ---------- debug ---------- */

  // 这个 debug 的线，正好差了一个周期，懒得整了，直接 RegNext

  io.dbg.wb_have_inst := RegNext(true.B)
  io.dbg.wb_pc        := RegNext(if_.io.out.pc_4 - 4.U)
  // printf("wb_pc=%x\n", io.debug.wb_pc)
  // printf("pc_4=%x\n", if_.io.out.pc_4)
  io.dbg.wb_ena := RegNext(Mux(cu_.io.ctrl.wb_sel =/= WB_sel.wbsel_X, true.B, false.B))
  io.dbg.wb_reg := RegNext(cur_inst(11, 7))
  // printf("wb_reg=%x\n", io.debug.wb_reg)
  // printf("wb_value=%x\n", io.debug.wb_value)
  io.dbg.wb_value := RegNext(
    MuxCase(
      0.U,
      Seq(
        (cu_.io.ctrl.wb_sel === WB_sel.wbsel_ALU) -> exe_.io.alu_out,
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
