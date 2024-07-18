package io.device

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import core.BusBundle
import io.HasSocParameter
import utils.ZeroExt

/** @brief
  *   这个是 CPU 的视角
  */
class BridgeDev_Bundle extends Bundle with HasCoreParameter {
  val addr  = Output(UInt(XLEN.W))
  val wen   = Output(UInt(dataBytes.W)) // 字节掩码
  val wdata = Output(UInt(XLEN.W))
  val rdata = Input(UInt(XLEN.W))
}

class Bridge(ranges: Seq[(Int /* addr_begin */, Int /* addr_end */ )]) extends Module with HasCoreParameter {

  /* ---------- io ---------- */

  val io = IO(new Bundle {
    val cpu = Flipped(new BusBundle) // 来自 cpu 的线
    val dev = Vec(ranges.size, new BridgeDev_Bundle) /* 这个是 bridge 的视角, DRAM 视角要 Flipped */
  })

  /* ---------- 判断 addr 落在了哪个区间内 ---------- */

  // FIXME 这里可能会有 reverse 的问题

  // 这是 one-hot 编码
  private val within_range = VecInit(ranges.map { case (beg, end) =>
    (ZeroExt(beg) <= io.cpu.addr && io.cpu.addr <= ZeroExt(end))
  })

  /* ---------- read ---------- */

  io.cpu.rdata := Mux1H(within_range, io.dev.map(_.rdata)) // 对应的设备, rdata

  /* ---------- write ---------- */

  for (i <- 0 until ranges.size) {
    io.dev(i).addr  := io.cpu.addr
    io.dev(i).wen   := Fill(dataBytes, within_range(i)) /* 只有第 i 个不是 0 */ & io.cpu.wen
    io.dev(i).wdata := io.cpu.wdata
    // printf("addr=%x\twdata=%x\ten=%b\n", io.cpu.addr, io.cpu.wdata, Fill(dataBytes, within_range(i)) & io.cpu.wen)
    // printf("within_range(i)=%b, io.cpu.wen=%b\n", within_range(i), io.cpu.wen)
  }

}

object CPUCore extends App with HasSocParameter with HasSevenSegParameter {
  val addr_space_range = Seq(
    (ADDR_MEM_BEGIN, ADDR_MEM_END), // memory
    (ADDR_DIG, ADDR_DIG + digitBytes), //  4 个 Byte
    (ADDR_LED, ADDR_LED + ledBytes), //  24 个 led
    (ADDR_SWITCH, ADDR_SWITCH + ledBytes), // 24 个 switch
    (ADDR_BUTTON, ADDR_BUTTON + buttonBytes) // 5 个 button
  )

  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new Bridge(addr_space_range),
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
