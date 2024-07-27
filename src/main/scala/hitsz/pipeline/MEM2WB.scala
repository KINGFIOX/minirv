package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.component.WB_sel
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter
import hitsz.component.WBBundle

class MEM2WB extends Bundle with HasCoreParameter with HasRegFileParameter {
  val wb = new WBBundle
  val data = new Bundle {
    val alu_out = Output(UInt(XLEN.W))
    val mem_out = Output(UInt(XLEN.W))
    val pc      = Output(UInt(XLEN.W))
  }
  val rf    = new RFRead
  val valid = Output(Bool())
}
