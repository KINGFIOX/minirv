package hitsz.io.trace

import chisel3._
import chisel3.util._
import hitsz.io.HasSocParameter
import hitsz.common.HasCoreParameter
import chisel3.util.experimental.loadMemoryFromFile
import firrtl.annotations.MemoryLoadFileType
import chisel3.util.experimental.loadMemoryFromFileInline

class DRAM(init_path: String, ty: MemoryLoadFileType = MemoryLoadFileType.Hex) extends Module with HasSocParameter with HasCoreParameter {
  val io = IO(new Bundle {
    val a   = Input(UInt(addrBits_verilator.W))
    val we  = Input(UInt(dataBytes.W))
    val d   = Input(UInt(XLEN.W))
    val spo = Output(UInt(XLEN.W))
  })

  val mem = Mem((1 << addrBits_verilator) >> dataBytesBits, UInt(XLEN.W))
  loadMemoryFromFileInline(mem, init_path, ty)

  val index = io.a >> dataBytesBits

  val data_in = io.d.asTypeOf(Vec(dataBytes, UInt(8.W)))

  /* ---------- write ---------- */

  val readData  = mem(index).asTypeOf(Vec(dataBytes, UInt(8.W)))
  val writeData = Wire(Vec(dataBytes, UInt(8.W)))

  for (i <- 0 until dataBytes) {
    writeData(i) := Mux(io.we(i), data_in(i), readData(i))
  }

  mem.write(index, writeData.asUInt)

  /* ---------- read ---------- */

  io.spo := mem.read(index)

}
