package hitsz.io.trace

import chisel3._
import chisel3.util._
import hitsz.io.HasSocParameter
import hitsz.common.HasCoreParameter
import chisel3.util.experimental.loadMemoryFromFile

class DRAM extends Module with HasSocParameter with HasCoreParameter {
  val io = IO(new Bundle {
    val a   = Input(UInt(addrBits_verilator.W))
    val we  = Input(UInt(dataBytes.W))
    val d   = Input(UInt(XLEN.W))
    val spo = Output(UInt(XLEN.W))
  })

  val mem = Mem((1 << addrBits_verilator) >> dataBytesBits, UInt(XLEN.W))
  loadMemoryFromFile(mem, "meminit.bin")

  val index = io.a >> dataBytesBits

  val mem_v = mem(index).asTypeOf(Vec(dataBytes, UInt(8.W)))

  val data_in = io.d.asTypeOf(Vec(dataBytes, UInt(8.W)))

  /* ---------- write ---------- */

  for (i <- 0 until dataBytes) {
    when(io.we(i)) {
      mem_v(i) := data_in(i)
    }
  }

  /* ---------- read ---------- */

  io.spo := mem_v.asUInt

}
