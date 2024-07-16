package common;

import chisel3._
import chisel3.util._

/** @brief
  *   一定是常量, 比方说 16进制的位宽
  */
trait Consts {}

/** @brief
  *   有几个通用寄存器
  */
trait HasRegFileParameter {
  val NRReg     = 32
  val NRRegbits = log2Up(NRReg)
}

trait HasECALLParameter {
  val ECALL_ADDRESS = 0x1c09_0000 /* 其实正常情况下, ecall 应该是调到 a7 指定的系统调用 */
}

trait HasDRAMParameter {
  val DRAM_BASE = 0x8000_0000 // TODO
  val DRAM_SIZE = 0x4000_0000
}

/** @brief
  *   有 core 的一些参数
  */
trait HasCoreParameter {
  implicit val XLEN: Int = 32 // 机器字长
  // val AddrBits  = XLEN // AddrBits is used in some cases
  // val DataBits  = XLEN // 一个 word 有几个 bit
  def dataBytes     = XLEN >> 3 // 一个 word 有几个字节
  def dataBytesBits = log2Ceil(dataBytes) // 一个 word 有几个字节的位宽
}
