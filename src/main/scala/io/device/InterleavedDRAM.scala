package io.device

import chisel3._
import chisel3.util._

/** @brief
  *   我设置了写使能, sw/sh/sb 只用设置不同的使能就行了。 这个是 Bridge 的视角
  * @param addr_w
  *   地址宽度
  * @param enable_w
  *   data_w % enable_w == 0
  * @param data_w
  *   数据宽度
  */
class DRAM_Bundle(val addrBits: Int, val enBits: Int = 4, val dataBits: Int) extends Bundle {
  require(dataBits % 8 == 0)
  require(dataBits / 8 == enBits)
  def subNum      = enBits // 4
  def subDataBits = dataBits / subNum // 32/4=8

  val addr  = Input(UInt(addrBits.W))
  val wen   = Input(UInt(enBits.W))
  val wdata = Input(UInt(dataBits.W))
  val rdata = Output(UInt(dataBits.W))
}

/** @brief
  *   不同的 dram 存储一个字的不同的部分, 第 0/1/2/3 字节。 注意: 我们的 InterleavedDram 的 addr 是 word 寻址的
  *
  * @param self_B
  */
class InterleavedDRAM(self_B: => DRAM_Bundle /* lazy parameter */ ) extends Module {
  private def sub_B = new DRAM_Bundle(self_B.addrBits /* 14 */, 1, self_B.subDataBits /* 8 */ )

  val io = IO(new Bundle {
    val self = self_B
    val subs = Vec(self_B.subNum, Flipped(sub_B))
  })

  private val wdata_vec = io.self.wdata.asTypeOf(Vec(self_B.subNum, UInt(self_B.subDataBits.W)))
  for (i <- 0 until self_B.subNum) {
    io.subs(i).addr  := io.self.addr
    io.subs(i).wen   := io.self.wen(i)
    io.subs(i).wdata := wdata_vec(i)
  }

  // FIXME 这里可能有 reverse
  io.self.rdata := VecInit(io.subs.map(_.rdata)).asUInt

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
