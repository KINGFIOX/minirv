package utils

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object MaskData {

  /** @brief
    *   其实这里应该叫做 Update(更新), 只用把没有被 mask 住的位置更新 。 被 mask 住的位置保持不变
    */
  def apply(oldData: UInt, newData: UInt, fullmask: UInt): UInt = {
    require(oldData.getWidth == newData.getWidth)
    require(oldData.getWidth == fullmask.getWidth)
    (newData & fullmask) /* 保留 newData 中 fullmask 为 1 的位 */ | (oldData & ~fullmask) /* 保护旧数据中被掩住的部分 */
  }
}

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

/** @brief
  *   two's complement 补码相加
  */
object TcAdd extends {
  def apply(lhs: UInt, rhs: UInt): UInt = (lhs.asSInt + rhs.asSInt).asUInt
}

/** @brief
  *   算术右移
  */
object TcSra extends {
  def apply(lhs: UInt, rhs: UInt): UInt = (lhs.asSInt >> rhs).asUInt
}

object GenMask {
  def apply(i: Int): UInt         = apply(i, i)
  def apply(i: Int, j: Int): UInt = ZeroExt(Fill(i - j + 1, true.B) << j)(64)
}
