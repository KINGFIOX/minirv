package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

/** @brief
  *   这个是 poll 总线的, 看 btn 的地址有什么信号。放到 Mem 阶段
  */
class IntPollU extends Module with HasCoreParameter {}
