package io.device

import chisel3._
import chisel3.util._

class BtnStbl(limit: Int) extends Module {
  val io = IO(new Bundle {
    val btn_in  = Input(Bool()) // 原始按钮输入信号
    val btn_out = Output(Bool()) // 消抖后的脉冲输出信号
  })

  /* ---------- default ---------- */

  io.btn_out := false.B

  /* ----------  ---------- */

  val btn_sync   = RegNext(RegNext(io.btn_in)) // 打两拍, 异步信号
  val btn_stable = RegInit(false.B)

  val (_, wrap) = Counter(btn_sync =/= btn_stable, limit)

  when(wrap) {
    btn_stable := btn_sync
    io.btn_out := true.B
  }

}

object BtnStbl extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new BtnStbl(15000),
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
