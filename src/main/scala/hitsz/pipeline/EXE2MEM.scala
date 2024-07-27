package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.component.MemUOpType
import hitsz.component.WB_sel
import hitsz.common.HasRegFileParameter
import hitsz.common.HasCoreParameter
import hitsz.component.WBBundle

class EXE2MEMBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  val mem     = Output(MemUOpType())
  val wb      = new WBBundle
  val rf      = new RFRead
  val alu_out = Output(UInt(XLEN.W))
  val pc      = Output(UInt(XLEN.W)) // 当前指令的地址
  val valid   = Output(Bool())
}

object EXE2MEMBundle {
  def apply(mem: MemUOpType.Type, wb: WBBundle, rf: RFRead, alu_out: UInt, pc: UInt, valid: Bool) = {
    val exe2mem_l = Wire(Flipped(new EXE2MEMBundle))
    exe2mem_l.mem     := mem // id2exe_r.mem
    exe2mem_l.wb      := wb // id2exe_r.wb
    exe2mem_l.rf      := rf // id2exe_r.rf
    exe2mem_l.alu_out := alu_out // alu_.io.out
    exe2mem_l.pc      := pc // id2exe_r.pc
    exe2mem_l.valid   := valid // id2exe_r.valid
    exe2mem_l
  }
}
