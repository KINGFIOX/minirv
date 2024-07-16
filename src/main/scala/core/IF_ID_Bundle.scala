package core

import chisel3._
import chisel3.util._
import common.HasCoreParameter

/** @brief
  *   IF -> ID 之间连接的线
  */
class IF_ID_Bundle extends Bundle with HasCoreParameter {
  val inst = Output(UInt(XLEN.W))
  val pc_4 = Output(UInt(XLEN.W))
}
