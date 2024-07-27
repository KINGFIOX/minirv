package hitsz.component

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter

object WB_sel extends ChiselEnum {
  val wbsel_X, wbsel_ALU, wbsel_CSR, wbsel_MEM, wbsel_PC4 /* jal & jalr */ = Value
}

class RFRead extends Bundle with HasCoreParameter with HasRegFileParameter {
  val rs1_i = Input(UInt(NRRegbits.W)) // 记录这些, 是因为要检测是否有 data hazard
  val rs1_v = Output(UInt(XLEN.W))
  val rs2_i = Input(UInt(NRRegbits.W))
  val rs2_v = Output(UInt(XLEN.W))
}

/** @brief
  *   寄存器堆, 但是其实不是一个 chisel 的 Module
  */
class RegFile extends Module with HasRegFileParameter with HasCoreParameter {
  val io = IO(new Bundle {
    val read = new RFRead
    val write = new Bundle {
      val rd_i  = Input(UInt(NRRegbits.W))
      val wdata = Input(UInt(XLEN.W))
      val wen   = Input(Bool())
      val valid = Input(Bool())
    }
  })
  private val _rf = Mem(NRReg, UInt(XLEN.W))

  /* ---------- rs1 read ---------- */
  io.read.rs1_v := Mux(
    io.read.rs1_i === 0.U, // cond
    0.U, // true
    Mux( // false
      io.write.valid,
      Mux( // true
        io.write.wen && io.write.rd_i === io.read.rs1_i, // cond
        /* wen=true && 并且相等 */ io.write.wdata, // true
        _rf(io.read.rs1_i) // false
      ),
      /* write invalid */ _rf(io.read.rs1_i) // false
    )
  )

  /* ---------- rs2 read ---------- */
  io.read.rs2_v := Mux(
    io.read.rs2_i === 0.U,
    0.U,
    Mux(
      io.write.valid,
      Mux(
        io.write.wen && io.write.rd_i === io.read.rs2_i,
        /* wen=true && 并且相等 */ io.write.wdata,
        _rf(io.read.rs2_i)
      ),
      /* write invalid */ _rf(io.read.rs2_i)
    )
  )

  /* ---------- rd write ---------- */
  when(io.write.valid) {
    when(io.write.wen && (io.write.rd_i =/= 0.U)) {
      _rf(io.write.rd_i) := io.write.wdata
    }
  }

  // def read(addr: UInt): UInt        = Mux(addr === 0.U, 0.U, _rf(addr))
  // def write(addr: UInt, data: UInt) = { _rf(addr) := Mux(addr === 0.U, 0.U, data(XLEN - 1, 0)) }
}
