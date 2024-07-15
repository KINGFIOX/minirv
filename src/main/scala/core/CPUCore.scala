package core

import chisel3._
import chisel3.util._
import component.IROM
import component.InstROMBundle

import _root_.debug.DebugBundle

/** @brief
  *   总线, 与 DRAM 交互的
  */
class BusBundle extends Bundle {
  val addr  = Output(UInt(32.W))
  val rdata = Input(UInt(32.W))
  val wen   = Output(UInt(4.W))
  val wdata = Output(UInt(32.W))
}

class CPUCore extends Module {
  val io = IO(new Bundle {
    val inst_rom = new InstROMBundle
    val bus      = new BusBundle
    // val debug    = new DebugBundle
  })

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
