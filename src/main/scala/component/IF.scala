/** @brief
  *   其实这个东西应该是: NPC + PC
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.HasECALLParameter
import core.IF_ID_Bundle

object NPCOpType extends ChiselEnum {
  val npc_X /* stall */, npc_4, npc_BR, npc_JAL, npc_JALR, npc_ECALL = Value
}

class InstROMBundle extends Bundle with HasCoreParameter {
  val addr = Output(UInt(XLEN.W)) // FIXME 对 IROM 传入指令的地址, 但是这个地址可能不是 32bit
  val inst = Input(UInt(XLEN.W)) // IROM 传出指令
}

/** @brief
  *   IF 阶段的输入信号
  */
class IFBundle extends Bundle with HasCoreParameter {
  val offset  = Input(UInt(XLEN.W))
  val br_flag = Input(Bool())
  val op      = Input(NPCOpType())
  val addr    = Input(UInt(XLEN.W)) // br/jal 是 offset ; jalr 就是绝对地址
}

class IF extends Module with HasCoreParameter with HasECALLParameter {
  val io = IO(new Bundle {
    val irom = new InstROMBundle
    val out  = new IF_ID_Bundle // 取指令输出, pc4 输出
    val in   = new IFBundle
    // val debug =
  })

  private val pc = RegInit(UInt(XLEN.W), 0.U) // pc = 0

  /* ---------- pc_cur ---------- */

  io.irom.addr := pc
  io.out.inst  := io.irom.inst
  io.out.pc4   := pc + 4.U

  /* ---------- pc_next ---------- */

  // 设置下一个时钟上升沿, pc

  switch(io.in.op) {
    is(NPCOpType.npc_X) {
      pc := pc
    }
    is(NPCOpType.npc_4) {
      pc := pc + 4.U
    }
    is(NPCOpType.npc_BR) {
      when(io.in.br_flag) {
        pc := io.in.addr + pc
      }
    }
    is(NPCOpType.npc_JAL) {
      pc := io.in.addr + pc
    }
    is(NPCOpType.npc_JALR) {
      pc := io.in.addr
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
