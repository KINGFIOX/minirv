package hitsz.utils

import hitsz.component.MemUOpType

import chisel3._
import chisel3.util._
import hitsz.pipeline.ID2EXEBundle
import hitsz.component.ALU_op1_sel
import hitsz.component.ALU_op2_sel
import hitsz.pipeline.EXE2MEMBundle
import hitsz.pipeline.MEM2WBBundle

object hazard {
  def isLoad(op: MemUOpType.Type): Bool = {
    op === MemUOpType.mem_LB || op === MemUOpType.mem_LH || op === MemUOpType.mem_LW || op === MemUOpType.mem_LBU || op === MemUOpType.mem_LHU
  }
  def is_ldRAW(id: ID2EXEBundle /* 新人 */, exe: ID2EXEBundle /* 老人 */ ): Bool = {
    (id.alu_ctrl.op1_sel === ALU_op1_sel.alu_op1sel_RS1 && id.rf.idxes.rs1 === exe.rf.idxes.rd && exe.rf.idxes.rd =/= 0.U) || (id.alu_ctrl.op2_sel === ALU_op2_sel.alu_op2sel_RS2 && id.rf.idxes.rs2 === exe.rf.idxes.rd && exe.rf.idxes.rd =/= 0.U)
  }
  // // write after write
  // // id 的时候, source 与 mem 和 exe 均相同, 此时选中 exe
  // def isWAW(exe: EXE2MEMBundle /* 老人 */, mem: MEM2WBBundle /* 老老人 */ ): Bool = {
  //   mem.rf.idxes.rd === exe.rf.idxes.rd && exe.rf.idxes.rd =/= 0.U
  // }
  def isRAW_rs1(id: ID2EXEBundle /* 新人 */, exe: EXE2MEMBundle /* 老人 */ ): Bool = {
    id.rf.idxes.rs1 === exe.rf.idxes.rd && exe.rf.idxes.rd =/= 0.U && exe.wb.wen
  }
  def isRAW_rs1(id: ID2EXEBundle /* 新人 */, mem: MEM2WBBundle /* 老人 */ ): Bool = {
    id.rf.idxes.rs1 === mem.rf.idxes.rd && mem.rf.idxes.rd =/= 0.U && mem.wen
  }
  def isRAW_rs2(id: ID2EXEBundle /* 新人 */, exe: EXE2MEMBundle /* 老人 */ ): Bool = {
    id.rf.idxes.rs2 === exe.rf.idxes.rd && exe.rf.idxes.rd =/= 0.U && exe.wb.wen
  }
  def isRAW_rs2(id: ID2EXEBundle /* 新人 */, mem: MEM2WBBundle /* 老人 */ ): Bool = {
    // printf(p"id.alu_ctrl.op2_sel=${id.alu_ctrl.op2_sel}\n")
    id.rf.idxes.rs2 === mem.rf.idxes.rd && mem.rf.idxes.rd =/= 0.U && mem.wen
  }
  def isRAW_rs1(rs1_i: UInt, exe: EXE2MEMBundle) = {
    rs1_i === exe.rf.idxes.rd && exe.rf.idxes.rd =/= 0.U && exe.wb.wen
  }
  def isRAW_rs1(rs1_i: UInt, mem: MEM2WBBundle) = {
    rs1_i === mem.rf.idxes.rd && mem.rf.idxes.rd =/= 0.U && mem.wen
  }
}
