package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.component.WB_sel
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter

class MEM2WB extends Bundle with HasCoreParameter with HasRegFileParameter {
  val ctrl = new Bundle {
    val wb_sel = Output(WB_sel())
    val wb_wen = Output(Bool())
  }
  val data = new Bundle {
    val alu_out = Output(UInt(XLEN.W))
    val mem_out = Output(UInt(XLEN.W))
    val pc      = Output(UInt(XLEN.W))
  }
  val rf = new Bundle {
    val rd_i  = Output(UInt(NRRegbits.W))
    val rs1_i = Output(UInt(NRRegbits.W))
    val rs2_i = Output(UInt(NRRegbits.W))
  }
}

object MEM2WB {

  def apply(exe2mem: EXE2MEMBundle, rdata: UInt): MEM2WB = {
    val mem2wb = Wire(Flipped(new MEM2WB))
    mem2wb.ctrl.wb_sel  := exe2mem.ctrl.wb_sel
    mem2wb.ctrl.wb_wen  := exe2mem.ctrl.wb_wen
    mem2wb.data.alu_out := exe2mem.alu_out
    mem2wb.data.mem_out := rdata
    mem2wb.data.pc      := exe2mem.pc
    mem2wb.rf.rd_i      := exe2mem.rd_i
    mem2wb.rf.rs1_i     := exe2mem.read_idx.rs1_i
    mem2wb.rf.rs2_i     := exe2mem.read_idx.rs2_i
    mem2wb
  }

}
