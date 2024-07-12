package component

import chisel3._
import chisel3.util._
import common.HasDRAMParameter
import common.HasCoreParameter

/** @brief
  *   不过, 这里可能要拓展, 如果是 64 位指令的话
  */
object MemUOpType extends ChiselEnum {
  val memu_LB, memu_LH, memu_LW, memu_LBU, memu_LHU, memu_SB, memu_SH, memu_SW = Value
}

/** @brief
  *   用来搞 data dram 的
  */
class MemU extends Module with HasCoreParameter with HasDRAMParameter {
  val io = IO(new Bundle {
    val offset = Input(UInt(XLEN.W)) // 立即数, 从 extu 出来
    val base   = Input(UInt(XLEN.W)) // 基地址
    val op     = Input(MemUOpType())
    val in     = Input(UInt(XLEN.W)) // 只有 store 的时候才有
    val out    = Output(UInt(XLEN.W)) // 只有 load 的时候才有
  })

  // TODO 暂时不清楚 dram 的 ip 核， 与总线的情况

}
