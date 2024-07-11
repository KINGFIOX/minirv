package utils

import chisel3._
import chisel3.util._
import common.HasCoreParameter

/** @brief
  *   有几个寄存器
  */
trait HasRegFileParameter {
  val NRReg = 32
}

/** @brief
  *   寄存器堆, 但是其实不是一个 chisel 的 Module
  */
class RegFile extends HasRegFileParameter with HasCoreParameter {
  val rf                            = Mem(NRReg, UInt(XLEN.W))
  def read(addr: UInt): UInt        = Mux(addr === 0.U, 0.U, rf(addr))
  def write(addr: UInt, data: UInt) = { rf(addr) := data(XLEN - 1, 0) }
}
