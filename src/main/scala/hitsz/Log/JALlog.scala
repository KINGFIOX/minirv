package hitsz.Log

import chisel3._
import hitsz.component.WB_sel

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
