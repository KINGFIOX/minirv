package hitsz.Log

import chisel3._
import hitsz.component.WB_sel
import hitsz.component.NPCOpType

trait Log {
  protected def generatedLookupTable[E <: ChiselEnum](enu: E, charIndex: Int): Vec[UInt] = {
    val defaultChar: Char = '?'
    val table = enu.all.map { enumVal =>
      val str = enumVal.toString
      if (charIndex < str.length) str(charIndex).toInt.U else defaultChar.toInt.U
    }
    VecInit(table)
  }
}

object JALlog extends Log {
  private val wb_sel_table = generatedLookupTable(WB_sel, 6)
  private val npc_op_table = generatedLookupTable(NPCOpType, 4)
  def apply(imm: UInt, npc_op: NPCOpType.Type, wb_sel: WB_sel.Type, rd_i: UInt): Unit = {
    val npc_op_char = npc_op_table(npc_op.asUInt)
    val wb_sel_char = wb_sel_table(wb_sel.asUInt)
    printf("---------- JAL ----------\n")
    printf("JAL: imm=%d, npc_op=%c, wb_sel=%c, rd_i=%d\n", imm, npc_op_char, wb_sel_char, rd_i)
    printf("----------  ----------\n")
  }
}
