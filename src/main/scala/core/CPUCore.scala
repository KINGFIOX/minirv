package core

import chisel3._
import chisel3.util._
import component.IROM
import component.InstROMBundle

import _root_.debug.DebugBundle
import component.IF
import component.BusBundle

class CPUCore extends Module {
  val io = IO(new Bundle {
    val inst_rom = new InstROMBundle
    val bus      = new BusBundle
    // val debug    = new DebugBundle
  })

  val if_ = Module(new IF)
  io.inst_rom := if_.io.irom

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
