package hitsz.io.trace

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter

/** @brief
  *   用于与 trace 交互
  */
class DebugBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  val wb_have_inst = Output(Bool()) // WB阶段是否有指令 (对单周期CPU，可在复位后恒为1)
  val wb_pc        = Output(UInt(XLEN.W)) // WB阶段的PC (若wb_have_inst=0，此项可为任意值)
  val wb_ena       = Output(Bool()) // WB阶段的寄存器写使能 (若wb_have_inst=0，此项可为任意值)
  val wb_reg       = Output(UInt(NRRegbits.W)) // WB阶段写入的寄存器号 (若wb_ena或wb_have_inst=0，此项可为任意值)
  val wb_value     = Output(UInt(XLEN.W)) // WB阶段写入寄存器的值 (若wb_ena或wb_have_inst=0，此项可为任意值)
}
