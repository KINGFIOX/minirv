/** @brief
  *   其实这个东西应该是: NPC + PC
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.HasECALLParameter
import core.IF_ID_Bundle
import io.blackbox.InstROMBundle

object NPCOpType extends ChiselEnum {
  val npc_X /* stall */, npc_4, npc_BR, npc_JAL, npc_JALR, npc_ECALL = Value
}

/** @brief
  *   IF 阶段的输入信号
  */
class IFBundle extends Bundle with HasCoreParameter {
  val imm     = Input(UInt(XLEN.W)) // br/jal 是 offset
  val br_flag = Input(Bool())
  val op      = Input(NPCOpType())
  val rs1_v   = Input(UInt(XLEN.W)) // jalr 就是绝对地址
}

class IF extends Module with HasCoreParameter with HasECALLParameter {
  val io = IO(new Bundle {
    val irom = Flipped(new InstROMBundle)
    val out  = new IF_ID_Bundle // 取指令输出, pc4 输出
    val in   = new IFBundle
    // val debug =
  })

  private val pc = RegInit(UInt(XLEN.W), 0.U) // pc = 0

  /* ---------- pc_cur ---------- */

  io.irom.addr := pc
  io.out.inst  := io.irom.inst
  io.out.pc_4  := pc + 4.U

  /* ---------- pc_next ---------- */

  // 设置下一个时钟上升沿, pc

  switch(io.in.op) {
    is(NPCOpType.npc_X) { /* 啥也不干 */ }
    is(NPCOpType.npc_4) {
      pc := pc + 4.U
    }
    is(NPCOpType.npc_BR) {
      when(io.in.br_flag) {
        pc := pc + io.in.rs1_v
      }.otherwise {
        pc := pc + 4.U
      }
    }
    is(NPCOpType.npc_JAL) {
      pc := pc + io.in.imm
    }
    is(NPCOpType.npc_JALR) {
      pc := (io.in.rs1_v + io.in.imm) & ~1.U(XLEN.W)
    }
    // is(NPCOpType.npc_ECALL) {
    //   pc := ECALL_ADDRESS.U
    // }
  }

}

object IF extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new IF,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
