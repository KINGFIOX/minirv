package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.HasRegFileParameter

object WB_sel extends ChiselEnum {
  val wbsel_X, wbsel_ALU, wbsel_CSR, wbsel_MEM, wbsel_PC4 /* jal & jalr */ = Value
}

/** @brief
  *   寄存器堆, 但是其实不是一个 chisel 的 Module
  */
private class RegFile extends HasRegFileParameter with HasCoreParameter {
  val rf                            = Mem(NRReg, UInt(XLEN.W))
  def read(addr: UInt): UInt        = Mux(addr === 0.U, 0.U, rf(addr))
  def write(addr: UInt, data: UInt) = { rf(addr) := Mux(addr === 0.U, 0.U, data(XLEN - 1, 0)) }
}
