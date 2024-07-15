package debug

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.HasRegFileParameter

/** @brief
  *   用于与 trace 交互
  */
class DebugBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  val wb_have_inst = Output(Bool())
  val wb_pc        = Output(UInt(XLEN.W))
  val wb_ena       = Output(Bool())
  val wb_reg       = Output(UInt(NRRegbits.W))
  val wb_value     = Output(UInt(XLEN.W))
}
