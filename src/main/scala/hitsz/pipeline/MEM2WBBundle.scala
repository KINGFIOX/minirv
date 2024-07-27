package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.component.WB_sel
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter
import hitsz.component.WBBundle

class MEM2WBBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  val wen   = Output(Bool())
  val wdata = Output(UInt(XLEN.W))
  val rf    = new RFRead
  val valid = Output(Bool())
}

object MEM2WBBundle {
  def apply(wen: Bool, wdata: UInt, rf: RFRead, valid: Bool) = {
    val mem2wb_l = Wire(Flipped(new MEM2WBBundle))
    mem2wb_l.wen   := wen // exe2mem_r.wb
    mem2wb_l.wdata := wdata // exe2mem_r.wdata
    mem2wb_l.rf    := rf // exe2mem_r.rf
    mem2wb_l.valid := valid // exe2mem_r.valid
    mem2wb_l
  }
}
