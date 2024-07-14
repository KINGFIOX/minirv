package core

import chisel3._
import chisel3.util._
import component.NPC
import component.PC
import component.IROM

class CPUCore extends Module {

  val npc  = Module(new NPC)
  val pc   = Module(new PC)
  val irom = Module(new IROM)
  pc.io.din    := npc.io.npc
  irom.io.addr := pc.io.pc

  val inst = irom.io.inst

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
