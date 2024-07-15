package io.device

import chisel3._
import chisel3.util._

/** @brief
  *   我设置了写使能, sw/sh/sb 只用设置不同的使能就行了
  *
  * @param addr_w
  *   地址宽度
  * @param enable_w
  *   data_w % enable_w == 0
  * @param data_w
  *   数据宽度
  */
class DRAM_Bundle(val addrBits: Int, val enBits: Int, val dataBits: Int) extends Bundle {
  require(dataBits % 8 == 0)
  require(dataBits / 8 == enBits)
  def subNum      = enBits // 分为多少 份
  def subDataBits = dataBits / subNum // 每一份数据的位宽是多少

  val addr         = Input(UInt(addrBits.W))
  val write_enable = Input(UInt(enBits.W)) // 这个一定是 字节掩码
  val write_data   = Input(UInt(dataBits.W))
  val read_data    = Output(UInt(dataBits.W))
}

/** @brief
  *   不同的 dram 存储一个字的不同的部分, 第 0/1/2/3 字节
  *
  * @param self_B
  */
class InterleavedDRAM(self_B: => DRAM_Bundle /* lazy parameter */ ) extends Module {
  private def sub_B = new DRAM_Bundle(self_B.addrBits /* 14 */, 1, self_B.subDataBits /* 8 */ )

  val io = IO(new Bundle {
    val subs = Vec(self_B.subNum, Flipped(sub_B)) // 这个是面向 ip core 的
    val self = self_B // 这个是面向 cpu core 的, 做了一层抽象: cpu core 就把这个 InterleavedDRAM 就看成是一个正常的 DRAM
  })

  // 这里变为向量, 更加便于操作
  private val read_data  = Wire(Vec(self_B.subNum, UInt(self_B.subDataBits.W)))
  private val write_data = io.self.write_data.asTypeOf(Vec(self_B.subNum, UInt(self_B.subDataBits.W)))

  for (i <- 0 until self_B.subNum) {
    io.subs(i).addr         := io.self.addr
    io.subs(i).write_enable := io.self.write_enable(i)
    io.subs(i).write_data   := write_data(i)
    read_data(i)            := io.subs(i).read_data
  }

  io.self.read_data := read_data.asUInt

}

object InterleavedDRAM extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new InterleavedDRAM(
      new DRAM_Bundle(14, 4, 32)
    ),
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
