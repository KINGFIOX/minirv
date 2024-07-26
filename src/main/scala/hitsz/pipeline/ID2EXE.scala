package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter

class ID2EXE extends Bundle with HasCoreParameter with HasRegFileParameter {
  val rf = new Bundle {
    val rs1_i = Output(UInt(NRRegbits.W)) // 记录这些, 是因为要检测是否有 data hazard
    val rs1_v = Output(UInt(XLEN.W))
    val rs2_i = Output(UInt(NRRegbits.W))
    val rs2_v = Output(UInt(XLEN.W))
    val rd_i  = Output(UInt(NRRegbits.W))
    val wen   = Output(Bool()) // 是否发生写, 当然只会在 wb 阶段才会写入
  }
}
