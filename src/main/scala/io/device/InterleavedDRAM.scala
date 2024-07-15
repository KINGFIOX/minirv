package io.device

import chisel3._
import chisel3.util._

/** @brief
  *
  * @param addr_w
  *   地址宽度
  * @param enable_w
  *   data_w % enable_w == 0
  * @param data_w
  *   数据宽度
  */
class DRAM_Bundle(val addr_w: Int /* 14 */, val enable_w: Int, val data_w: Int) extends Bundle {
  val addr         = Input(UInt(addr_w.W))
  val write_enable = Input(UInt(enable_w.W))
  val write_data   = Input(UInt(data_w.W))
  val read_data    = Output(UInt(data_w.W))
}

/** @brief
  *
  * @param self_B
  */
class InterleavedDRAM(self_B: => DRAM_Bundle /* lazy parameter */ ) extends Module {
  require(self_B.data_w % self_B.enable_w == 0) // assert 数据的位宽能被平分
  private val sub_cnt    = self_B.enable_w // 4
  private val sub_data_w = self_B.data_w / sub_cnt // 32 / 4 = 8

  private def sub_B = new DRAM_Bundle(self_B.addr_w /* 14 */, 1, sub_data_w /* 8 */ )

  val io = IO(new Bundle {
    val subs = Vec(sub_cnt, Flipped(sub_B)) // 这个是面向 ip core 的
    val self = self_B // 这个是面向 cpu core 的, 做了一层抽象: cpu core 就把这个 InterleavedDRAM 就看成是一个正常的 DRAM
  })

  // 这里变为向量, 更加便于操作
  private val read_data  = Wire(Vec(sub_cnt, UInt(sub_data_w.W)))
  private val write_data = Wire(Vec(sub_cnt, UInt(sub_data_w.W)))

  for (i <- 0 until sub_cnt) {
    io.subs(i).addr         := io.self.addr
    io.subs(i).write_enable := io.self.write_enable(i)
    io.subs(i).write_data   := write_data(i)
    read_data(i)            := io.subs(i).read_data
  }

  // connect the read_data/write_data
  val w = sub_data_w
  for (i <- 0 until sub_cnt) {
    write_data(i) := io.self.write_data(w * (i + 1) - 1, w * i)
  }
  io.self.read_data := read_data.asUInt
}
