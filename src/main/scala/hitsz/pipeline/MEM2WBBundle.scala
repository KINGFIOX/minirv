package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.component.WB_sel
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter
import hitsz.component.WBBundle

class MEM2WBBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  val wb = new WBBundle
  val data = new Bundle {
    val alu_out = Output(UInt(XLEN.W))
    val mem_out = Output(UInt(XLEN.W))
    val pc      = Output(UInt(XLEN.W))
  }
  val rf    = new RFRead
  val valid = Output(Bool())
}

object MEM2WBBundle {
  def apply(wb: WBBundle, alu_out: UInt, mem_out: UInt, pc: UInt, rf: RFRead, valid: Bool) = {
    val mem2wb_l = Wire(Flipped(new MEM2WBBundle))
    mem2wb_l.wb           := wb // exe2mem_r.wb
    mem2wb_l.data.alu_out := alu_out // exe2mem_r.alu_out
    mem2wb_l.data.mem_out := mem_out // mem_.io.rdata
    mem2wb_l.data.pc      := pc // exe2mem_r.pc
    mem2wb_l.rf           := rf // exe2mem_r.rf
    mem2wb_l.valid        := valid // exe2mem_r.valid
    mem2wb_l
  }
}
