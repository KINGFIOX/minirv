package hitsz.utils

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter

/** 注意: left -> right, 这是单向的
  */
object pipe {
  def apply[T <: Data](left: T, right: T, wen: Bool) = {
    right := RegEnable(left, wen)
  }
  def apply[T <: Data](left: T, wen: Bool): T = {
    RegEnable(left, wen)
  }
}
