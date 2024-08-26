package hitsz.io.verilator

import chisel3._
import chisel3.util._
import hitsz.io.HasSocParameter
import hitsz.common.HasCoreParameter
import chisel3.util.experimental.loadMemoryFromFile

/** @brief
  *   这些就不用参数化了, 已经写死了
  */
class DRAM(init_path: String) extends Module with HasSocParameter with HasCoreParameter {
  val io = IO(new Bundle {
    val a   = Input(UInt(verilator_addrBits.W)) // word 寻址
    val we  = Input(UInt(dataBytes.W))
    val d   = Input(UInt(XLEN.W))
    val spo = Output(UInt(XLEN.W))
  })

  val size   = verilator_dramLens * dataBytes
  val memory = Mem(size, UInt(8.W))
  loadMemoryFromFile(memory, init_path)

  val addr = io.a << dataBytesBits

  io.spo := memory(addr + 3.U) ## memory(addr + 2.U) ## memory(addr + 1.U) ## memory(addr)

  for (i <- 0 until dataBytes) {
    when(io.we(i.U)) {
      memory.write(addr, io.d(i * 8 + 7, i * 8))
    }
  }

}

object DRAM extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new DRAM("./start.bin"),
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
