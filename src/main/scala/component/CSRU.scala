/** @brief
  *   放到 EXE 阶段
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.HasCSRRegFileParameter

object CSRUOpType extends ChiselEnum {
  val csru_X, csru_CSRRW, csru_CSRRS, csru_CSRRC = Value
}

/** @brief
  *   寄存器堆, 但是其实不是一个 chisel 的 Module
  */
class CSRRegFile extends HasCoreParameter with HasCSRRegFileParameter {
  val rf                            = Mem(NCSRReg, UInt(XLEN.W))
  def read(addr: UInt): UInt        = Mux(addr === 0.U, 0.U, rf(addr))
  def write(addr: UInt, data: UInt) = { rf(addr) := data(XLEN - 1, 0) }
}

/** @brief
  *   用于 CSR 操作的模块
  */
class CSRU extends Module with HasCoreParameter with HasCSRRegFileParameter {
  val io = IO(new Bundle {
    val op  = Input(CSRUOpType())
    val csr = Input(UInt(NCSRbits.W))
    val rs1 = Input(UInt(XLEN.W)) // Mux(控制信号 , zimm, rs1)
    val out = Output(UInt(XLEN.W)) // 读取出来 CSR 的值, rd 由 controller 控制
  })

  val csr_regfile = new CSRRegFile // 内部维护一个 csr_regfile

  io.out := 0.U

  switch(io.op) {
    is(CSRUOpType.csru_X) {
      io.out := 0.U
    }
    is(CSRUOpType.csru_CSRRW) { // csr read & write
      val t = csr_regfile.read(io.csr)
      csr_regfile.write(io.csr, io.rs1)
      io.out := t
    }
    is(CSRUOpType.csru_CSRRS) {
      val t = csr_regfile.read(io.csr)
      csr_regfile.write(io.csr, t | io.rs1)
      io.out := t
    }
    is(CSRUOpType.csru_CSRRC) {
      val t = csr_regfile.read(io.csr)
      csr_regfile.write(io.csr, t & ~io.rs1)
      io.out := t
    }
  }
}
