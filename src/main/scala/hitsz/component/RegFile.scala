package hitsz.component

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter

object WB_sel extends ChiselEnum {
  val wbsel_X, wbsel_ALU, wbsel_CSR, wbsel_MEM, wbsel_PC4 /* jal & jalr */ = Value
}

/** @brief
  *   寄存器堆, 但是其实不是一个 chisel 的 Module
  */
class RegFile extends Module with HasRegFileParameter with HasCoreParameter {
  val io = IO(new Bundle {
    val inst  = Input(UInt(XLEN.W))
    val rs1_v = Output(UInt(XLEN.W))
    val rs2_v = Output(UInt(XLEN.W))
    val wdata = Input(UInt(XLEN.W))
    val wen   = Input(Bool())
  })
  private val _rf = Mem(NRReg, UInt(XLEN.W))

  val rs1_i = io.inst(19, 15)
  val rs2_i = io.inst(24, 20)
  val rd_i  = io.inst(11, 7)

  io.rs1_v := Mux(rs1_i === 0.U, 0.U, _rf(rs1_i))
  io.rs2_v := Mux(rs2_i === 0.U, 0.U, _rf(rs2_i))

  when(io.wen && (rd_i =/= 0.U)) {
    _rf(rd_i) := io.wdata
  }

  // def read(addr: UInt): UInt        = Mux(addr === 0.U, 0.U, _rf(addr))
  // def write(addr: UInt, data: UInt) = { _rf(addr) := Mux(addr === 0.U, 0.U, data(XLEN - 1, 0)) }
}
