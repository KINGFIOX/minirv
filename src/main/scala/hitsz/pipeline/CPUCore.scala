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
import hitsz.io.trace.DebugBundle
import hitsz.utils.pipe
import hitsz.component.JMPBundle
import hitsz.component.BRUOpType

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

  /* ---------- ---------- IF ---------- ---------- */

  private val if_ = Module(new IF) // Instruction Fetch: NPC + PC
  io.irom <> if_.io.irom

  private val if_l = if_.io.out
  /* ---------- ---------- ID ---------- ---------- */
  val if_r = pipe(if_l, true.B)
  // val inst_r = inst_l

  // cu input
  private val cu_ = Module(new CU)
  cu_.io.inst := if_r.inst

  // reg file read
  private val rs1_i    = if_r.inst(19, 15)
  private val rs2_i    = if_r.inst(24, 20)
  private val regfile_ = Module(new RegFile)
  regfile_.io.read.rs1_i := rs1_i
  regfile_.io.read.rs2_i := rs2_i
  private val rs1_v = regfile_.io.read.rs1_v
  private val rs2_v = regfile_.io.read.rs2_v
  private val rd_i  = if_r.inst(11, 7)

  // jmp
  if_.io.jmp := JMPBundle(cu_.io.jmp_op, cu_.io.imm, if_r.pc, rs1_v) // 传递给 if_ ， 为了处理 无条件跳转的 控制冒险

  // id_br
  if_.io.br.id_isBr := (cu_.io.bru_op =/= BRUOpType.bru_X)

  val id2exe_l = Wire(Flipped(new ID2EXEBundle))
  id2exe_l.pc       := if_r.pc
  id2exe_l.valid    := if_r.valid
  id2exe_l.rf       := RFRead(rs1_i, rs2_i, rd_i, rs1_v, rs2_v)
  id2exe_l.alu_ctrl := cu_.io.alu_ctrl
  id2exe_l.bru_op   := cu_.io.bru_op
  id2exe_l.wb       := cu_.io.wb
  id2exe_l.mem      := cu_.io.mem
  id2exe_l.imm      := cu_.io.imm
  /* ---------- ---------- EXE ---------- ---------- */
  private val id2exe_r = Wire(new ID2EXEBundle)
  pipe(id2exe_l, id2exe_r, true.B)
  // id2exe_r := id2exe_l // 只有一部分会进入到 exe 中

  // ***** alu *****
  private val alu_ = Module(new ALU)
  alu_.io.alu_op := id2exe_r.alu_ctrl.calc
  alu_.io.op1_v := Mux1H(
    Seq(
      (id2exe_r.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_ZERO) -> 0.U,
      (id2exe_r.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_RS1)  -> id2exe_r.rf.vals.rs1,
      (id2exe_r.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_PC)   -> (id2exe_r.pc)
    )
  )
  alu_.io.op2_v := Mux1H(
    Seq(
      (id2exe_r.alu_ctrl.op2_sel === ALU_op2_sel.alu_op2sel_ZERO) -> 0.U,
      (id2exe_r.alu_ctrl.op2_sel === ALU_op2_sel.alu_op2sel_IMM)  -> id2exe_r.imm,
      (id2exe_r.alu_ctrl.op2_sel === ALU_op2_sel.alu_op2sel_RS2)  -> id2exe_r.rf.vals.rs2
    )
  )

  // ***** bru *****
  // beq rs1, rs2, offset => if(rs1==rs2) pc=pc+offset
  private val bru_ = Module(new BRU)
  bru_.io.in.rs1_v  := id2exe_r.rf.vals.rs1
  bru_.io.in.rs2_v  := id2exe_r.rf.vals.rs2
  bru_.io.in.bru_op := id2exe_r.bru_op

  // if_ <- exe_br
  if_.io.br.exe_br.br_flag := bru_.io.br_flag
  if_.io.br.exe_br.imm     := id2exe_r.imm
  if_.io.br.exe_br.pc      := id2exe_r.pc

  private val exe2mem_l = Wire(Flipped(new EXE2MEMBundle))
  exe2mem_l.mem     := id2exe_r.mem
  exe2mem_l.wb      := id2exe_r.wb
  exe2mem_l.rf      := id2exe_r.rf
  exe2mem_l.alu_out := alu_.io.out
  exe2mem_l.pc      := id2exe_r.pc
  exe2mem_l.valid   := id2exe_r.valid
  /* ---------- ---------- MEM ---------- ---------- */
  private val exe2mem_r = Wire(new EXE2MEMBundle)
  pipe(exe2mem_l, exe2mem_r, true.B)
  // exe2mem_r := exe2mem_l

  val mem_ = Module(new MemU)
  mem_.io.bus <> io.bus
  mem_.io.in.op    := exe2mem_r.mem
  mem_.io.in.addr  := exe2mem_r.alu_out
  mem_.io.in.wdata := exe2mem_r.rf.vals.rs2
  mem_.io.valid    := exe2mem_r.valid

  private val mem2wb_l = Wire(Flipped(new MEM2WB))
  mem2wb_l.wb           := exe2mem_r.wb
  mem2wb_l.data.alu_out := exe2mem_r.alu_out
  mem2wb_l.data.pc      := exe2mem_r.pc
  mem2wb_l.data.mem_out := mem_.io.rdata
  mem2wb_l.rf           := exe2mem_r.rf
  mem2wb_l.valid        := exe2mem_r.valid
  /* ---------- ---------- WB ---------- ---------- */
  private val mem2wb_r = Wire(new MEM2WB)
  pipe(mem2wb_l, mem2wb_r, true.B)
  // mem2wb_r := mem2wb_l

  // 写入只读寄存器
  regfile_.io.write.rd_i  := mem2wb_r.rf.idxes.rd
  regfile_.io.write.wen   := mem2wb_r.wb.wen
  regfile_.io.write.valid := mem2wb_r.valid
  regfile_.io.write.wdata := MuxCase(
    0.U,
    Seq(
      (mem2wb_r.wb.sel === WB_sel.wbsel_ALU, mem2wb_r.data.alu_out),
      (mem2wb_r.wb.sel === WB_sel.wbsel_MEM, mem2wb_r.data.mem_out),
      (mem2wb_r.wb.sel === WB_sel.wbsel_PC4, mem2wb_r.data.pc + 4.U)
    )
  )

  /* ---------- debug ---------- */

  io.dbg.wb_have_inst := true.B
  io.dbg.wb_pc        := mem2wb_r.data.pc
  io.dbg.wb_ena       := regfile_.io.write.wen
  io.dbg.wb_reg       := regfile_.io.write.rd_i
  io.dbg.wb_value     := regfile_.io.write.wdata
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
