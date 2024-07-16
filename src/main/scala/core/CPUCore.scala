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
  io.inst_rom := if_.io.irom

  private val cur_inst = if_.io.out.inst
  private val pc_4     = if_.io.out.pc_4

  /* ---------- ID ---------- */

  val cu_ = Module(new CU)
  cu_.io.inst := cur_inst

  val regfile_ = new RegFile

  val rs1 = regfile_.read(cu_.io.rf.rs1)
  val rs2 = regfile_.read(cu_.io.rf.rs2)
  val imm = cu_.io.imm

  /* ---------- EXE ---------- */

  val alu_ = Module(new ALU)
  alu_.io.alu_op := cu_.io.ctrl.alu_op
  alu_.io.op1 := Mux1H(
    Seq(
      (cu_.io.ctrl.op1_sel === OP1_sel.op1sel_ZERO) -> 0.U,
      (cu_.io.ctrl.op1_sel === OP1_sel.op1sel_RS1)  -> rs1,
      (cu_.io.ctrl.op1_sel === OP1_sel.op1sel_PC)   -> (if_.io.out.pc_4 - 4.U)
    )
  )
  alu_.io.op2 := Mux1H(
    Seq(
      (cu_.io.ctrl.op2_sel === OP2_sel.op2sel_ZERO) -> 0.U,
      (cu_.io.ctrl.op2_sel === OP2_sel.op2sel_SEXT) -> imm,
      (cu_.io.ctrl.op2_sel === OP2_sel.op2sel_RS2)  -> regfile_.read(cu_.io.rf.rs2)
    )
  )

  /* ---------- MEM ---------- */

  val mem_ = Module(new MemU)
  mem_.io.in.op    := cu_.io.ctrl.op_mem
  mem_.io.in.addr  := alu_.io.out
  mem_.io.in.wdata := rs2

  /* ---------- WB ---------- */

  val rd = cu_.io.rf.rd

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
