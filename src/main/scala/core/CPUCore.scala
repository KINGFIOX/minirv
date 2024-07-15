package core

import chisel3._
import chisel3.util._
import component.IROM
import component.InstROMBundle

class BusBundle extends Bundle {
  val addr  = Output(UInt(32.W))
  val rdata = Input(UInt(32.W))
  val wen   = Output(UInt(4.W))
  val wdata = Output(UInt(32.W))
}

/** @brief
  *   用于与 trace 交互
  */
class DebugBundle extends Bundle {
  val wb_have_inst = Output(Bool())
  val wb_pc        = Output(UInt(32.W))
  val wb_ena       = Output(Bool())
  val wb_reg       = Output(UInt(5.W))
  val wb_value     = Output(UInt(32.W))
}

class CPUCore extends Module {
  val io = IO(new Bundle {
    val inst_rom = new InstROMBundle
    val bus      = new BusBundle
    val debug    = new DebugBundle
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
