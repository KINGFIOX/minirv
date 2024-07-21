package hitsz.io

import chisel3._
import chisel3.util._

import hitsz.io.blackbox.DistributedSinglePortRAM
import hitsz.io.blackbox.DistributedSinglePortROM
import hitsz.common.HasCoreParameter
import hitsz.io.device.HasSevenSegParameter
import hitsz.io.blackbox.PLL
import hitsz.io.blackbox.CLKGen
import hitsz.core.CPUCore
import hitsz.io.device.Bridge
import hitsz.io.trace.IROM
import hitsz.io.trace.DRAM
import hitsz.io.device.SevenSegDigital
import hitsz.io.DebugBundle
import hitsz.io.device.BridgeDev_Bundle
import hitsz.io.device.BtnStbl
import hitsz.io.device.PosEdge

/* ********** ********** Settings ********** ********** */

object ENV {
  // 定义: 是输出到 vivado, 还是 verilator
  val isVivado = false
}

/* ********** ********** 外设设置 ********** ********** */

trait HasSocParameter {

  /* ---------- vivado ---------- */

  val addrBits_vivado = 14
  def iromLens_vivado = 1 << addrBits_vivado // rom 有 (1 << 14) 个 word
  def dramLens_vivado = 1 << addrBits_vivado

  /* ---------- verilator ---------- */

  val addrBits_verilator = 16
  def iromLens_verilator = 1 << addrBits_vivado // rom 有 (1 << 14) 个 word
  def dramLens_verilator = 1 << addrBits_vivado

  /* ---------- irom ---------- */

  val USER_BEGIN = 0
  def USER_LEN   = if (ENV.isVivado) iromLens_vivado else iromLens_verilator

  val KERNEL_BEGIN = 0x0_1c09_0000
  def KERNEL_LEN   = if (ENV.isVivado) iromLens_vivado else iromLens_verilator

  /* ---------- ---------- */

  val switchBits = 24
  val buttonBits = 5
  val ledBits    = 24 // 因为有 24 个  led 灯

  val btnStbl = 100 // 稳定按钮的

  def divCeil(x: Int, y: Int): Int = (x + y - 1) / y

  def switchBytes = divCeil(switchBits, 8) // 3
  def buttonBytes = divCeil(buttonBits, 8) // 1
  def ledBytes    = divCeil(ledBits, 8) // 3

  /* ---------- 地址空间 ---------- */

  val ADDR_DIG       = 0x0_ffff_f000 // 注意一下, 这些范围是负的, java/scala 只有 int, 没有 unsigned int
  val ADDR_LED       = 0x0_ffff_f060
  val ADDR_SWITCH    = 0x0_ffff_f070
  val ADDR_BUTTON    = 0x0_ffff_f078
  val ADDR_MEM_BEGIN = 0x0_0000_0000
  val ADDR_MEM_END   = 0x0_ffff_f000 // memory map io
}

/* ********** ********** 外设设置 ********** ********** */

import hitsz.io.blackbox.CLKGen

class miniRV_SoC extends RawModule with HasSevenSegParameter with HasSocParameter with HasCoreParameter {

  val io = FlatIO(new Bundle {
    val fpga_clk = Input(Clock())
    val fpga_rst = Input(Bool())

    // /* ---------- 外设 ---------- */

    val swit   = Input(UInt(switchBits.W)) // 拨码开关
    val button = Input(UInt(buttonBits.W)) // 中间 5 个按钮

    // // 8 个数码管
    val dig_en = Output(UInt(digitNum.W))
    val DN_A   = Output(Bool())
    val DN_B   = Output(Bool())
    val DN_C   = Output(Bool())
    val DN_D   = Output(Bool())
    val DN_E   = Output(Bool())
    val DN_F   = Output(Bool())
    val DN_G   = Output(Bool())
    val DN_DOT = Output(Bool())
    // //
    val led = Output(UInt(ledBits.W))

    /* ---------- debug ---------- */

    val dbg = new DebugBundle
  })

  private val cpu_clk = if (ENV.isVivado) CLKGen(io.fpga_clk) else io.fpga_clk

  withClockAndReset(cpu_clk, io.fpga_rst) {

    /* ---------- CPU Core ---------- */

    val cpu_core = Module(new CPUCore)

    /* ---------- irom ---------- */

    if (ENV.isVivado) { // vivado
      // irom
      val irom = Module(new DistributedSinglePortROM(iromLens_vivado, XLEN))
      irom.io.a             := cpu_core.io.irom.addr(addrBits_vivado - 1, dataBytesBits)
      cpu_core.io.irom.inst := irom.io.spo
    } else { // verilator
      val irom = Module(new IROM)
      irom.io.a             := cpu_core.io.irom.addr(addrBits_verilator - 1, dataBytesBits)
      cpu_core.io.irom.inst := irom.io.spo
    }

    /* ---------- bridge ---------- */

    val addr_space_range = Seq(
      (ADDR_MEM_BEGIN, ADDR_MEM_END), // memory
      (ADDR_DIG, ADDR_DIG + digitBytes), //  4 个 Byte
      (ADDR_LED, ADDR_LED + ledBytes), //  24 个 led
      (ADDR_SWITCH, ADDR_SWITCH + switchBytes), // 24 个 switch
      (ADDR_BUTTON, ADDR_BUTTON + buttonBytes) // 5 个 button
    )

    val bridge = Module(new Bridge(addr_space_range))
    bridge.io.cpu <> cpu_core.io.bus

    // ***** dram *****

    val bus0 = bridge.io.dev(0) // 第一个设备: dram

    if (ENV.isVivado) {
      // vivado 就是用 interleaved dram
    } else {
      val dram = Module(new DRAM)
      dram.io.a  := bus0.addr(addrBits_verilator - 1, dataBytesBits) // dram 是 word 寻址的
      dram.io.d  := bus0.wdata
      dram.io.we := bus0.wen
      bus0.rdata := dram.io.spo
    }

    // ***** digit *****

    val bus1 = bridge.io.dev(1)
    val dig  = Module(new SevenSegDigital)
    dig.io.input_en := bus1.wen
    dig.io.input    := bus1.wdata
    bus1.rdata      := 0.U // 不用给 CPU 的输入

    io.dig_en := dig.io.led_enable // 第几个 enable
    io.DN_A   := dig.io.led.AN
    io.DN_B   := dig.io.led.BN
    io.DN_C   := dig.io.led.CN
    io.DN_D   := dig.io.led.DN
    io.DN_E   := dig.io.led.EN
    io.DN_F   := dig.io.led.FN
    io.DN_G   := dig.io.led.GN
    io.DN_DOT := dig.io.led.dot

    // ***** 24 个 led *****

    val bus2 = bridge.io.dev(2)
    val reg  = RegInit(VecInit(Seq.fill(3)(0.U(8.W))))
    for (i <- 0 until 3) {
      when(bus2.wen(i)) {
        reg(i) := bus2.wdata(8 * i + 7, 8 * i)
      }
    }
    io.led     := reg.asUInt
    bus2.rdata := 0.U // 不用给 CPU 的输入

    // ***** 24 个 拨码开关 *****

    val bus3 = bridge.io.dev(3)
    bus3.rdata := Cat(0.U((32 - 24).W), io.swit)

    // ***** 5 个 按钮 *****

    val bus4 = bridge.io.dev(4)
    bus4.rdata := PosEdge(0.U((32 - 5).W) ## BtnStbl(btnStbl, io.button))

    io.dbg := cpu_core.io.debug
  }
}

object miniRV_SoCTest extends App {

  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new miniRV_SoC,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
