package hitsz.component

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.utils.SignExt
import hitsz.utils.ZeroExt
import hitsz.pipeline.BusBundle

/** @brief
  *   不过, 这里可能要拓展, 如果是 64 位指令的话
  */
object MemUOpType extends ChiselEnum {
  val mem_X, mem_LB, mem_LH, mem_LW, mem_LBU, mem_LHU, mem_SB, mem_SH, mem_SW = Value
}

/** @brief
  *   Mem 的输入
  */
class EXE_MEM_Bundle extends Bundle with HasCoreParameter {
  val op    = Input(MemUOpType())
  val addr  = Input(UInt(XLEN.W))
  val wdata = Input(UInt(XLEN.W))
}

/** @brief
  *   这里访存包含 MMIO, 其中我要求, 访问外设要 lbu, lb, sb
  */
class MemU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val in    = new EXE_MEM_Bundle
    val bus   = new BusBundle // 与总线连接, CPUCore 也要有这个
    val rdata = Output(UInt(XLEN.W))
  })

  /* ---------- default output ---------- */

  io.bus.addr  := Cat(io.in.addr(XLEN - 1, dataBytesBits), 0.U(dataBytesBits.W))
  io.bus.wdata := io.in.wdata
  io.bus.wen   := 0.U
  io.rdata     := 0.U

  /* ---------- data struct ---------- */

  private val rdataVec = io.bus.rdata.asTypeOf(Vec(dataBytes, UInt(8.W)))
  private val subword  = io.in.addr(dataBytesBits - 1, 0) /* (1, 0) */
  val subword1H        = UIntToOH(subword, dataBytes /* 4 */ )

  /* ---------- 控制 ---------- */

  /** @brief
    *   注意, 可以抛出不对齐的异常
    */

  switch(io.in.op) {
    is(MemUOpType.mem_X) { /* 啥也不干, 上面的默认值 */ }
    is(MemUOpType.mem_LB) {
      printf("lb=%x\n", io.bus.rdata)
      io.rdata := SignExt(rdataVec(subword))
    }
    is(MemUOpType.mem_LH) { // 2 对齐
      when(subword === 0.U || subword === 2.U) {
        io.rdata := SignExt(rdataVec(subword + 1.U) ## rdataVec(subword))
      }.otherwise {
        printf("Unaligned memory access at %x\n", io.in.addr)
      }
    }
    is(MemUOpType.mem_LW) {
      when(subword === 0.U) {
        io.rdata := rdataVec.asUInt
      }.otherwise {
        printf("Unaligned memory access at %x\n", io.in.addr)
      }
    }
    is(MemUOpType.mem_LBU) {
      io.rdata := ZeroExt(rdataVec(subword))
    }
    is(MemUOpType.mem_LHU) {
      when(subword === 0.U || subword === 2.U) {
        io.rdata := ZeroExt(rdataVec(subword + 1.U) ## rdataVec(subword))
      }.otherwise {
        printf("Unaligned memory access at %x\n", io.in.addr)
      }
    }
    is(MemUOpType.mem_SB) {
      val byte = io.in.wdata(7, 0) // 取出来一个字节
      val zero = 0.U(XLEN.W).asTypeOf(Vec(dataBytes, UInt(8.W)))
      zero(subword) := byte
      io.bus.wdata  := zero.asUInt
      io.bus.wen    := subword1H
    }
    is(MemUOpType.mem_SH) {
      when(subword === 0.U) {
        val byte2 = io.in.wdata(15, 0)
        io.bus.wdata := 0.U(16.W) ## byte2
        io.bus.wen   := "b0011".U(dataBytes.W) // 害, 这个参数化, 难绷
      }
      when(subword === 2.U) {
        val byte2 = io.in.wdata(15, 0)
        io.bus.wdata := byte2 ## 0.U(16.W)
        io.bus.wen   := "b1100".U(dataBytes.W)
      }
      when(subword === 1.U || subword === 3.U) {
        printf("Unaligned memory access at %x\n", io.in.addr)
      }
    }
    is(MemUOpType.mem_SW) {
      when(subword === 0.U) {
        io.bus.wen := "b1111".U(dataBytes.W) // 害, 这个参数化, 难绷
      }.otherwise {
        printf("Unaligned memory access at %x\n", io.in.addr)
      }
    }
  }
}

object MemU extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new MemU,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
