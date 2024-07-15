package io.device

/** @brief
  *   copy from 萝老
  */

import chisel3._
import chisel3.util._
import common.HasCoreParameter

trait HasSevenSegParameter {
  val cycle    = 100000
  val enBits   = 4 // 字节掩码的位宽, 位宽是多少, 那么就有几个字节
  val inBits   = enBits * 8
  val digitNum = 8 // 一共有 8 个数码管
}

class SevenSeg_Bundle extends Bundle {
  val dot  = Output(Bool())
  val bits = Output(UInt(7.W)) // 7 段数码管, 硬编码, 就是这样的

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
    val data = Input(UInt(4.W)) // 一个 16 进制的宽度, 硬编码
    val led  = new SevenSeg_Bundle
  })

  io.led.bits := ~MuxCase(
    0.U(7.W),
    Seq(
      (io.data === 0x0.U) -> "b111_1110".U(7.W),
      (io.data === 0x1.U) -> "b011_0000".U(7.W),
      (io.data === 0x2.U) -> "b110_1101".U(7.W),
      (io.data === 0x3.U) -> "b111_1001".U(7.W),
      (io.data === 0x4.U) -> "b011_0011".U(7.W),
      (io.data === 0x5.U) -> "b101_1011".U(7.W),
      (io.data === 0x6.U) -> "b101_1111".U(7.W),
      (io.data === 0x7.U) -> "b111_0000".U(7.W),
      (io.data === 0x8.U) -> "b111_1111".U(7.W),
      (io.data === 0x9.U) -> "b111_1011".U(7.W),
      (io.data === 0xa.U) -> "b111_0111".U(7.W),
      (io.data === 0xb.U) -> "b001_1111".U(7.W),
      (io.data === 0xc.U) -> "b100_1110".U(7.W),
      (io.data === 0xd.U) -> "b011_1101".U(7.W),
      (io.data === 0xe.U) -> "b100_1111".U(7.W),
      (io.data === 0xf.U) -> "b100_0111".U(7.W)
    )
  )
  io.led.dot := true.B
}

/** @brief
  *   分时复用数码管
  */
class SevenSegDigital extends Module with HasCoreParameter with HasSevenSegParameter {
  val io = IO(new Bundle {
    val input_en   = Input(UInt(enBits.W)) // 字节掩码
    val input      = Input(UInt(XLEN.W))
    val led_enable = Output(UInt(digitNum.W))
    val led        = new SevenSeg_Bundle
  })

  require(io.input_en.getWidth * 8 == io.input.getWidth)

  private val reg      = RegInit(VecInit(Seq.fill(4)(0.U(8.W))))
  private val inputVec = io.input.asTypeOf(Vec(digitNum, UInt(8.W)))
  for (i <- 0 until 4) {
    when(io.input_en(i)) {
      reg(i) := inputVec(i)
    }
  }

  /* ---------- 控制哪一个 digit 使能 ---------- */

  val cnt  = Counter(cycle)
  val wrap = cnt.inc()

  private val enable_reg = Module(new CycleShiftRegister(8))
  enable_reg.io.next := wrap

  private val led_en = enable_reg.io.out
  io.led_enable := ~led_en

  /* ---------- 控制输出的内容 ---------- */

  private val decoder    = Module(new DigDecoder) // hex -> 数码管
  private val reg_bits_v = reg.asUInt.asTypeOf(Vec(XLEN / io.input_en.getWidth, UInt(4.W)))
  // decoder 的输入连着一个 mux
  decoder.io.data := Mux1H(Seq.tabulate(8) { i =>
    led_en(i) -> reg_bits_v(i)
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
