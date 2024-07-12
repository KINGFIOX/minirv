/** @brief
  *   这里面会进行一次计算
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.HasECALLParameter

object NPCOpType extends ChiselEnum {
  val npc_X /* stall */, npc_4, npc_BR, npc_JAL, npc_JALR, npc_ECALL = Value
}

/** @brief
  *   jalr 来自于 alu , jal 来自于 alu, BR 直接来自于 sext(imm)
  */
class NPC extends Module with HasCoreParameter with HasECALLParameter {
  val io = IO(new Bundle {
    val pc      = Input(UInt(XLEN.W)) // 先用 XLEN 吧
    val op      = Input(NPCOpType())
    val addr    = Input(UInt(XLEN.W)) // br/jal 是 offset ; jalr 就是绝对地址
    val br_flag = Input(Bool()) // 来自于 BRU
    val npc     = Output(UInt(XLEN.W))
    val pc4     = Output(UInt(XLEN.W))
  })

  io.pc4 := io.pc + 4.U // 这个是输出 pc4

  io.npc := io.pc // 先默认保持不变

  switch(io.op) {
    is(NPCOpType.npc_X) {
      io.npc := io.pc
    }
    is(NPCOpType.npc_4) {
      io.npc := io.pc + 4.U
    }
    is(NPCOpType.npc_BR) {
      when(io.br_flag) {
        io.npc := io.addr + io.pc
      }
    }
    is(NPCOpType.npc_JAL) {
      io.npc := io.addr + io.pc
    }
    is(NPCOpType.npc_JALR) {
      io.npc := io.addr
    }
    is(NPCOpType.npc_ECALL) {
      io.npc := ECALL_ADDRESS.U
    }
  }
}

object NPC extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new NPC,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
