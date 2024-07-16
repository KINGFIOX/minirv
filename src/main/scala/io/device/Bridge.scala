package io.device

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import core.BusBundle

/** @brief
  *   这个是 CPU 的视角
  */
class BridgeDev_Bundle extends Bundle with HasCoreParameter {
  val addr  = Output(UInt(XLEN.W))
  val wen   = Output(UInt(dataBytes.W)) // 字节掩码
  val wdata = Output(UInt(XLEN.W))
  val rdata = Input(UInt(XLEN.W))
}

class Bridge(ranges: Seq[(BigInt /* addr_begin */, BigInt /* addr_end */ )]) extends Module {
  def deviceNum = ranges.size

  if (deviceNum > 10) {
    println("Warning: too many sub-devices")
  }

  val io = IO(new Bundle {
    val cpu     = Flipped(new BusBundle) // 来自 cpu 的线
    val devices = Vec(deviceNum, new BridgeDev_Bundle) /* 这个是 bridge 的视角, DRAM 视角要 Flipped */
  })

  private val addr = io.cpu.addr

  /* ---------- 判断 addr 落在了哪个区间内 ---------- */

  private val within_range = Wire(Vec(deviceNum, Bool()))
  ranges.zipWithIndex.foreach { case ((beg, end), idx) =>
    within_range(idx) := (beg.U <= addr && addr < end.U)
  }

  io.devices.zipWithIndex.foreach { case (b, i) =>
    b.addr  := addr
    b.wen   := Fill(4, within_range(i)) /* 只有第 i 个不是 0 */ & io.cpu.wen
    b.wdata := io.cpu.wdata
  }
  io.cpu.rdata := Mux1H(within_range, io.devices.map(_.rdata))
}
