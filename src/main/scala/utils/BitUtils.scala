package utils

import chisel3._
import chisel3.util._

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
