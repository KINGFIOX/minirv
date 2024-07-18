package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import core.BusBundle
import io.HasSocParameter

/** @brief
  *   这个是 poll 总线的, 看 btn 的地址有什么信号。放到 Mem 阶段
  */
class IntPollU extends Module with HasCoreParameter with HasSocParameter {
  val io = IO(new Bundle {
    val bus = new BusBundle
    val no  = Output(UInt(XLEN.W)) // 中断的编号
    // 相应的, 我可以发出中断信号
  })

  io.bus.wen   := 0.U // 只能是 disable
  io.bus.wdata := 0.U // 只能是 0
  io.bus.addr  := ADDR_BUTTON.asSInt.asUInt

  io.no := io.bus.rdata

  // TODO 生成控制信号

}
