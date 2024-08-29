package hitsz.io.verilator

import chisel3._
import chisel3.util._
import hitsz.io.HasSocParameter
import hitsz.common.HasCoreParameter
import chisel3.util.experimental.loadMemoryFromFile

class DRAM(init_path: String) extends BlackBox {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val a   = Input(UInt(16.W))
    val we  = Input(UInt(4.W))
    val d   = Input(UInt(32.W))
    val spo = Output(UInt(32.W))
  })

}
