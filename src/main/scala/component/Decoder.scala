/** @brief
  *   这个文件定义了控制信号，实现了 Decoder
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

/** @brief
  *   解码，会生成一排控制信号。然后也会进行符号拓展操作
  */
class Decoder extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    // output
  })

}
