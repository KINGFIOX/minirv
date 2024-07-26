package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.component.MemUOpType
import hitsz.component.WB_sel
import hitsz.common.HasRegFileParameter
import hitsz.common.HasCoreParameter

class EXE2MEMBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  val ctrl = new Bundle {
    val op_mem = Output(MemUOpType())
    val wb_sel = Output(WB_sel())
    val wb_wen = Output(Bool())
  }
  val read_val = new Bundle {
    val rs2_v = Output(UInt(XLEN.W))
  }
  val read_idx = new Bundle {
    val rs1_i = Output(UInt(NRRegbits.W))
    val rs2_i = Output(UInt(NRRegbits.W))
  }
  val rd_i    = Output(UInt(NRRegbits.W))
  val alu_out = Output(UInt(XLEN.W))
  val pc      = Output(UInt(XLEN.W)) // 当前指令的地址
}

object EXE2MEMBundle {
  def apply(id2exe: ID2EXEBundle, alu_out: UInt) = {
    val exe2mem = Wire(Flipped(new EXE2MEMBundle))
    exe2mem.ctrl.op_mem    := id2exe.cu.ctrl.op_mem //
    exe2mem.ctrl.wb_sel    := id2exe.cu.ctrl.wb_sel
    exe2mem.ctrl.wb_wen    := id2exe.cu.ctrl.wb_wen
    exe2mem.read_val.rs2_v := id2exe.rf.read_val.rs2_v //
    exe2mem.read_idx       := id2exe.rf.read_idx //
    exe2mem.rd_i           := id2exe.rf.rd_i //
    exe2mem.alu_out        := alu_out //
    exe2mem.pc             := id2exe.pc //
    exe2mem
  }
}
