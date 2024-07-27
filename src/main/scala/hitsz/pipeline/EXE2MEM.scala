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
