package io.device

/** @brief
  *   copy from 萝老
  */

import chisel3._
import chisel3.util._
import common.HasCoreParameter

trait HasSevenSegParameter {
  val cycle = 100000
}

class SevenSeg_Bundle extends Bundle {
  val dot  = Output(Bool())
  val bits = Output(UInt(7.W))

  def AN = bits(6)
  def BN = bits(5)
  def CN = bits(4)
  def DN = bits(3)
  def EN = bits(2)
  def FN = bits(1)
  def GN = bits(0)
}

class DigDecoder extends Module {
  val io = IO(new Bundle {
    val data = Input(UInt(4.W))
    val led  = new SevenSeg_Bundle
  })

  io.led.bits := ~MuxCase(
    0.U(7.W),
    Seq(
      (io.data === 0x0.U) -> "b1111110".U(7.W),
      (io.data === 0x1.U) -> "b0110000".U(7.W),
      (io.data === 0x2.U) -> "b1101101".U(7.W),
      (io.data === 0x3.U) -> "b1111001".U(7.W),
      (io.data === 0x4.U) -> "b0110011".U(7.W),
      (io.data === 0x5.U) -> "b1011011".U(7.W),
      (io.data === 0x6.U) -> "b1011111".U(7.W),
      (io.data === 0x7.U) -> "b1110000".U(7.W),
      (io.data === 0x8.U) -> "b1111111".U(7.W),
      (io.data === 0x9.U) -> "b1111011".U(7.W),
      (io.data === 0xa.U) -> "b1110111".U(7.W),
      (io.data === 0xb.U) -> "b0011111".U(7.W),
      (io.data === 0xc.U) -> "b1001110".U(7.W),
      (io.data === 0xd.U) -> "b0111101".U(7.W),
      (io.data === 0xe.U) -> "b1001111".U(7.W),
      (io.data === 0xf.U) -> "b1000111".U(7.W)
    )
  )
  io.led.dot := true.B
}

/** @brief
  *   分时复用数码管
  */
class SevenSegDigital extends Module with HasCoreParameter with HasSevenSegParameter {
  val io = IO(new Bundle {
    val input_en   = Input(UInt(4.W)) // 字节掩码
    val input      = Input(UInt(XLEN.W))
    val led_enable = Output(UInt(8.W))
    val led        = new SevenSeg_Bundle
  })

  private val reg = RegInit(VecInit(Seq.fill(4)(0.U(8.W))))
  for (i <- 0 until 4) {
    when(io.input_en(i)) {
      reg(i) := io.input(8 * i + 7, 8 * i)
    }
  }

  val cnt  = Counter(cycle)
  val wrap = cnt.inc()

  private val enable_reg = Module(new CycleShiftRegister(8))
  enable_reg.io.next := wrap

  private val led_en = enable_reg.io.out
  io.led_enable := ~led_en

  private val reg_bits = reg.asUInt
  private val decoder  = Module(new DigDecoder)
  decoder.io.data := Mux1H(Seq.tabulate(8) { i =>
    led_en(i) -> reg_bits(4 * i + 3, 4 * i)
  })
  io.led := decoder.io.led
}

/** @brief
  *   循环移位寄存器
  */
class CycleShiftRegister(width: Int) extends Module {
  val io = IO(new Bundle {
    val next = Input(Bool())
    val out  = Output(UInt(width.W))
  })

  private val reg = RegInit(1.U(width.W))

  when(io.next) {
    reg := reg(width - 2, 0) ## reg(width - 1)
  }

  io.out := reg
}

object SevenSegDigital extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new SevenSegDigital,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
