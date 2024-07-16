package core

import chisel3._
import chisel3.util._
import component.IROM

import _root_.debug.DebugBundle
import component.IF
import common.HasCoreParameter
import component.RegFile
import component.CU

class InstROMBundle extends Bundle with HasCoreParameter {
  val addr = Output(UInt(XLEN.W)) // FIXME 对 IROM 传入指令的地址, 但是这个地址可能不是 32bit
  val inst = Input(UInt(XLEN.W)) // IROM 传出指令
}

/** @brief
  *   总线, 与 DRAM 交互的
  */
class BusBundle extends Bundle with HasCoreParameter {
  val addr  = Output(UInt(XLEN.W))
  val rdata = Input(UInt(XLEN.W))
  val wen   = Output(UInt(dataBytes.W))
  val wdata = Output(UInt(XLEN.W))
}

class CPUCore extends Module {
  val io = IO(new Bundle {
    val inst_rom = new InstROMBundle
    val bus      = new BusBundle
    // val debug    = new DebugBundle
  })

  /* ---------- IF ---------- */

  val if_ = Module(new IF) // Instruction Fetch: NPC + PC
  io.inst_rom := if_.io.irom

  private val cur_inst = if_.io.out.inst
  private val pc_4     = if_.io.out.pc_4

  /* ---------- ID ---------- */

  val regfile = new RegFile

  val cu_ = Module(new CU)

}

object CPUCore extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new CPUCore,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
