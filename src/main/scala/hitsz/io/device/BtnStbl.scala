package hitsz.io.device

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

object BtnStbl {
  def apply(btnStbl: Int, btn_in: Bool): Bool = {
    val btn = Module(new BtnStbl(btnStbl))
    btn.io.btn_in := btn_in
    btn.io.btn_out
  }
  def apply(btnStbl: Int, btn_in_v: UInt): UInt = {
    val btnStable               = btn_in_v.asBools.map(apply(btnStbl, _))
    val btnStableVec: Vec[Bool] = VecInit(btnStable)
    btnStableVec.asUInt
  }
}
