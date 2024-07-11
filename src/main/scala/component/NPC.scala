package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object NPCOpType extends ChiselEnum {
  val npc_4, npc_ECALL, npc_ = Value
}

class NPC extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val pc   = Input(UInt(XLEN.W)) // 先用 XLEN 吧
    val op   = Input(NPCOpType())
    val addr = Input(UInt(XLEN.W)) // jalr 是绝对地址, 其他是来自于 ALU
    val npc  = Output(UInt(XLEN.W))
    val pc4  = Output(UInt(XLEN.W))
  })

  io.pc4 := io.pc + 4.U
}
