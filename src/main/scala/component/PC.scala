package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

class PC extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val din = Input(UInt(XLEN.W))
    val pc  = Output(UInt(XLEN.W))
    // TODO 这个是需要连接 clk 和 rst 的
    // 注意 chisel 与 fpga ip core 的交互
  })

  // TODO
}
