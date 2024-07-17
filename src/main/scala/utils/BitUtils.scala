package utils

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object ZeroExt extends HasCoreParameter {
  def apply(a: UInt)(implicit XLEN: Int) = {
    val aLen = a.getWidth
    if (aLen >= XLEN) a(XLEN - 1, 0) else Cat(0.U((XLEN - aLen).W), a)
  }
}

object SignExt {
  def apply(a: UInt)(implicit XLEN: Int): UInt = {
    val aLen    = a.getWidth
    val signBit = a(aLen - 1)
    if (aLen >= XLEN) a(XLEN - 1, 0) else Cat(Fill(XLEN - aLen, signBit), a)
  }
}
