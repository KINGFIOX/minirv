package io.device

import chisel3._
import chisel3.util._
import core.BusBundle
import common.HasCoreParameter

class BridgeDev_Bundle extends Bundle with HasCoreParameter {
  val addr  = Output(UInt(XLEN.W))
  val wen   = Output(UInt(dataBytes.W)) // 字节掩码
  val wdata = Output(UInt(XLEN.W))
  val rdata = Input(UInt(XLEN.W))
}

class Bridge(ranges: Seq[(BigInt, BigInt)]) extends Module {
  if (ranges.size > 10) {
    println("Warning: too many sub-devices")
  }

  val io = IO(new Bundle {
    val cpu     = Flipped(new BusBundle) // 来自 cpu 的线
    val devices = Vec(ranges.size, new BridgeDev_Bundle)
  })

  private val addr         = io.cpu.addr
  private val within_range = Wire(Vec(ranges.size, Bool()))
  ranges.zipWithIndex.foreach { case ((beg, end), idx) =>
    within_range(idx) := (beg.U <= addr && addr < end.U)
  }

  io.devices.zipWithIndex.foreach { case (b, i) =>
    b.addr  := addr
    b.wen   := Fill(4, within_range(i)) & io.cpu.wen
    b.wdata := io.cpu.wdata
  }
  io.cpu.rdata := Mux1H(within_range, io.devices.map(_.rdata))
}
