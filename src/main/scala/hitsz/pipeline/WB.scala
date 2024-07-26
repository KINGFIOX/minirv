package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.component.WB_sel
import hitsz.common.HasRegFileParameter

class WB extends Module with HasCoreParameter with HasRegFileParameter {

  val io = IO(new Bundle {
    val in = new Bundle {
      val ctrl = new Bundle {
        val wb_sel = Input(WB_sel())
        val wb_wen = Input(Bool())
      }
      val data = new Bundle {
        val alu_out = Input(UInt(XLEN.W))
        val mem_out = Input(UInt(XLEN.W))
        val pc      = Input(UInt(XLEN.W)) // 这里 pc 没有计算
      }
      val rd_i = Input(UInt(NRRegbits.W))
    }
    val out = new Bundle {
      val wdata = Output(UInt(XLEN.W))
      val wen   = Output(Bool())
      val rd_i  = Output(UInt(NRRegbits.W))
    }
  })

  io.out.wdata := 0.U
  io.out.wen   := io.in.ctrl.wb_wen // cu_.io.ctrl.wb_wen
  io.out.rd_i  := io.in.rd_i
  switch(io.in.ctrl.wb_sel) {
    is(WB_sel.wbsel_X) {
      io.out.rd_i := 0.U
      /* 啥也不干 */
    }
    is(WB_sel.wbsel_ALU) {
      io.out.wdata := io.in.data.alu_out
    }
    is(WB_sel.wbsel_CSR) { /* TODO */ }
    is(WB_sel.wbsel_MEM) {
      io.out.wdata := io.in.data.mem_out
    }
    is(WB_sel.wbsel_PC4) {
      io.out.wdata := io.in.data.pc + 4.U
    }
  }

}
