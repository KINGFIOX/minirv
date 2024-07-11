package common;

import chisel3._
import chisel3.util._

/** @brief
  *   有几个通用寄存器
  */
trait HasRegFileParameter {
  val NRReg = 32
}

/** @brief
  *   有 core 的一些参数
  */
trait HasCoreParameter {
  val XLEN = 32 // 机器字长
  // val AddrBits  = XLEN // AddrBits is used in some cases
  // val DataBits  = XLEN // 一个 word 有几个 bit
  val DataBytes = XLEN >> 3 // 一个 word 有几个字节
}
