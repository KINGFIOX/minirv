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
import hitsz.utils.hazard
import hitsz.common.HasRegFileParameter

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
  private val regfile_ = Module(new RegFile)
  regfile_.io.read.rs1_i := if_r.inst(19, 15)
  regfile_.io.read.rs2_i := if_r.inst(24, 20)

  // id_br
  if_.io.br.id_isBr := (cu_.io.bru_op =/= BRUOpType.bru_X)

  private val id2exe_l = ID2EXEBundle(
    if_r.pc,
    if_r.valid,
    RFRead(if_r.inst(19, 15), if_r.inst(24, 20), if_r.inst(11, 7), regfile_.io.read.rs1_v, regfile_.io.read.rs2_v),
    cu_.io.alu_ctrl,
    cu_.io.bru_op,
    cu_.io.wb,
    cu_.io.mem,
    cu_.io.imm
  )

  /* ---------- ---------- EXE ---------- ---------- */
  // private val id2exe_r = Wire(new ID2EXEBundle)
  private val id2exe_r = pipe(id2exe_l, true.B)
  // pipe(id2exe_l, id2exe_r, true.B)
  // id2exe_r := id2exe_l // 只有一部分会进入到 exe 中

  // ***** alu *****
  private val alu_ = Module(new ALU)
  alu_.io.alu_op := id2exe_r.alu_ctrl.calc
  alu_.io.op1_v := Mux1H(
    Seq(
      (id2exe_r.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_ZERO) -> 0.U,
      (id2exe_r.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_PC)   -> (id2exe_r.pc),
      (id2exe_r.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_RS1)  -> id2exe_r.rf.vals.rs1
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

  private val exe2mem_l = EXE2MEMBundle(
    id2exe_r.mem,
    id2exe_r.wb,
    id2exe_r.rf,
    alu_.io.out,
    id2exe_r.pc,
    id2exe_r.valid
  )
  /* ---------- ---------- MEM ---------- ---------- */
  // private val exe2mem_r = Wire(new EXE2MEMBundle)
  private val exe2mem_r = pipe(exe2mem_l, true.B)
  // pipe(exe2mem_l, exe2mem_r, true.B)
  // exe2mem_r := exe2mem_l

  val mem_ = Module(new MemU)
  mem_.io.bus <> io.bus
  mem_.io.in.op    := exe2mem_r.mem
  mem_.io.in.addr  := exe2mem_r.alu_out
  mem_.io.in.wdata := exe2mem_r.rf.vals.rs2
  mem_.io.valid    := exe2mem_r.valid

  private val mem2wb_l = MEM2WBBundle(
    exe2mem_r.wb.wen,
    MuxCase(
      0.U,
      Seq(
        (exe2mem_r.wb.sel === WB_sel.wbsel_ALU, exe2mem_r.alu_out),
        (exe2mem_r.wb.sel === WB_sel.wbsel_MEM, mem_.io.rdata),
        (exe2mem_r.wb.sel === WB_sel.wbsel_PC4, exe2mem_r.pc + 4.U)
      )
    ),
    exe2mem_r.rf,
    exe2mem_r.valid
  )
  /* ---------- ---------- WB ---------- ---------- */
  // private val mem2wb_r = Wire(new MEM2WBBundle)
  private val mem2wb_r = pipe(mem2wb_l, true.B)
  // pipe(mem2wb_l, mem2wb_r, true.B)
  // mem2wb_r := mem2wb_l

  // 写入只读寄存器
  regfile_.io.write.rd_i  := mem2wb_r.rf.idxes.rd
  regfile_.io.write.wen   := mem2wb_r.wen
  regfile_.io.write.valid := mem2wb_r.valid
  regfile_.io.write.wdata := mem2wb_r.wdata

  /* ---------- ---------- hazard ---------- ---------- */

  // ***** mem-id data hazard *****
  if_.io.ld_hazard.pc       := id2exe_l.pc
  if_.io.ld_hazard.happened := false.B
  when(hazard.is_ldRAW(id2exe_l, id2exe_r) && hazard.isLoad(id2exe_r.mem)) {
    id2exe_l.valid            := false.B // 把新人废了
    if_.io.ld_hazard.happened := true.B
  }

  // ***** WAW *****
  when(hazard.isRAW_rs1(id2exe_l, exe2mem_l)) {
    id2exe_l.rf.vals.rs1 := exe2mem_l.alu_out
    // printf(p"rs1=exe2mem_l.alu_out=${exe2mem_l.alu_out}\n")
  }.elsewhen(hazard.isRAW_rs1(id2exe_l, mem2wb_l)) {
    id2exe_l.rf.vals.rs1 := mem2wb_l.wdata
    // printf(p"rs1=mem2wb_l.wdata=${mem2wb_l.wdata}\n")
  }
  when(hazard.isRAW_rs2(id2exe_l, exe2mem_l)) {
    id2exe_l.rf.vals.rs2 := exe2mem_l.alu_out
    // printf(p"rs2=exe2mem_l.alu_out=${exe2mem_l.alu_out}\n")
  }.elsewhen(hazard.isRAW_rs2(id2exe_l, mem2wb_l)) {
    id2exe_l.rf.vals.rs2 := mem2wb_l.wdata
    // printf(p"rs2=mem2wb_l.wdata=${mem2wb_l.wdata}\n")
  }

  // jmp -> jalr/jal
  if_.io.jmp := JMPBundle(
    cu_.io.jmp_op,
    cu_.io.imm,
    if_r.pc,
    MuxCase(
      regfile_.io.read.rs1_v,
      Seq(
        (hazard.isRAW_rs1(if_r.inst(19, 15), exe2mem_l)) -> exe2mem_l.alu_out,
        (hazard.isRAW_rs1(if_r.inst(19, 15), mem2wb_l))  -> mem2wb_l.wdata
      )
    )
  ) // 传递给 if_ ， 为了处理 无条件跳转的 控制冒险

  /* ---------- debug ---------- */

  io.dbg.wb_have_inst := mem2wb_r.valid
  // io.dbg.wb_have_inst := true.B
  io.dbg.wb_pc    := RegNext(exe2mem_r.pc)
  io.dbg.wb_ena   := mem2wb_r.wen
  io.dbg.wb_reg   := mem2wb_r.rf.idxes.rd
  io.dbg.wb_value := mem2wb_r.wdata

  // printf("========== pc = %x ==========\n", RegNext(exe2mem_r.pc))
  // for (i <- 0 until 32 by 4) {
  //   printf(p"x(${i}) = 0x${Hexadecimal(regfile_.io.dbg(i.U))}, ")
  //   printf(p"x(${i + 1}) = 0x${Hexadecimal(regfile_.io.dbg((i + 1).U))}, ")
  //   printf(p"x(${i + 2}) = 0x${Hexadecimal(regfile_.io.dbg((i + 2).U))}, ")
  //   printf(p"x(${i + 3}) = 0x${Hexadecimal(regfile_.io.dbg((i + 3).U))}\n")
  // }

  io.dbg.inst_valid := mem2wb_r.valid

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
