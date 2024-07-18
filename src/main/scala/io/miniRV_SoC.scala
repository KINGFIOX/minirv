import chisel3._
import chisel3.util._
import io.blackbox.DistributedSinglePortRAM
import io.blackbox.DistributedSinglePortROM
import common.HasCoreParameter
import io.device.HasSevenSegParameter
import io.blackbox.PLL
import io.blackbox.CLKGen
import core.CPUCore
import io.device.Bridge
import io.blackbox.IROM
import io.trace.DRAM
import io.device.SevenSegDigital
import io.DebugBundle

trait HasSocParameter {
  val addrBits = 14
  def iromLens = 1 << addrBits // rom 有 (1 << 14) 个 word
  def dramLens = 1 << addrBits

  val switchBits = 24
  val buttonBits = 5
  val ledBits    = 24 // 因为有 24 个  led 灯

  def divCeil(x: Int, y: Int): Int = (x + y - 1) / y

  def switchBytes = divCeil(switchBits, 8) // 3
  def buttonBytes = divCeil(buttonBits, 8) // 1
  def ledBytes    = divCeil(ledBits, 8) // 3

  val ADDR_DIG       = 0xffff_f000
  val ADDR_LED       = 0xffff_f060
  val ADDR_SWITCH    = 0xffff_f070
  val ADDR_BUTTON    = 0xffff_f078
  val ADDR_MEM_BEGIN = 0x0000_0000
  val ADDR_MEM_END   = 0xffff_f000
}

object ENV {
  // 定义: 是输出到 vivado, 还是 verilator
  val isVivado = false
}

import io.blackbox.CLKGen

class miniRV_SoC extends RawModule with HasSevenSegParameter with HasSocParameter with HasCoreParameter {

  val io = FlatIO(new Bundle {
    val fpga_clk = Input(Clock())
    val fpga_rst = Input(Bool())

    // /* ---------- 外设 ---------- */

    // val switch = Input(UInt(switchBits.W)) // 拨码开关
    // val button = Input(UInt(buttonBits.W)) // 中间 5 个按钮
    // // 8 个数码管
    // val dig_en = Output(UInt(digitNum.W))
    // val DN_A   = Output(Bool())
    // val DN_B   = Output(Bool())
    // val DN_C   = Output(Bool())
    // val DN_D   = Output(Bool())
    // val DN_E   = Output(Bool())
    // val DN_F   = Output(Bool())
    // val DN_G   = Output(Bool())
    // val DN_DP  = Output(Bool())
    // //
    // val led = Output(UInt(ledBits.W))

    /* ---------- debug ---------- */

    val debug = new DebugBundle
  })

  val cpu_clk = if (ENV.isVivado) CLKGen(io.fpga_clk) else io.fpga_clk

  withClockAndReset(cpu_clk, io.fpga_rst) {

    /* ---------- CPU Core ---------- */

    val cpu_core = Module(new CPUCore)

    /* ---------- irom ---------- */

    if (ENV.isVivado) { // vivado

      // irom
      val irom = Module(new DistributedSinglePortROM(iromLens, XLEN))
      irom.io.a             := cpu_core.io.irom.addr(addrBits - 1, dataBytesBits)
      cpu_core.io.irom.inst := irom.io.spo

    } else { // verilator

      // 这里没有用 bridge, 直接搞了

      val irom = Module(new IROM)
      irom.io.a             := cpu_core.io.irom.addr(15, 2)
      cpu_core.io.irom.inst := irom.io.spo

      val dram = Module(new DRAM)
      dram.io.clk           := io.fpga_clk
      dram.io.a             := cpu_core.io.bus.addr(15, 2) // dram 是 word 寻址的
      dram.io.d             := cpu_core.io.bus.wdata
      dram.io.we            := cpu_core.io.bus.wen
      cpu_core.io.bus.rdata := dram.io.spo
      io.debug              := cpu_core.io.debug
    }

    // /* ---------- bridge ---------- */

    // val addr_space_range: Seq[(BigInt, BigInt)] = Seq(
    //   (ADDR_MEM_BEGIN, ADDR_MEM_END), // memory
    //   (ADDR_DIG, ADDR_DIG + digitBytes), //  4 个 Byte
    //   (ADDR_LED, ADDR_LED + ledBytes), //  24 个 led
    //   (ADDR_SWITCH, ADDR_SWITCH + ledBytes), // 24 个 switch
    //   (ADDR_BUTTON, ADDR_BUTTON + buttonBytes) // 5 个 button
    // )

    // val bridge = Module(new Bridge(addr_space_range))
    // bridge.io.cpu := cpu_core.io.bus

    // /* ---------- dram ---------- */

    // val bus0 = bridge.io.devices(0)
    // val dram = Module(new DRAM)
    // dram.io.clk := io.fpga_clk
    // dram.io.a   := bus0.addr(15, 0)
    // dram.io.d   := bus0.wdata
    // dram.io.we  := bus0.wen
    // bus0.rdata  := dram.io.spo

    // /* ---------- Seven 数码管 ---------- */

    // val bus1 = bridge.io.devices(1)
    // val dig  = Module(new SevenSegDigital)
    // dig.io.input_en := bus1.wen
    // dig.io.input    := bus1.wdata
    // io.dig_en       := dig.io.led_enable
    // io.DN_A         := dig.io.led.AN
    // io.DN_B         := dig.io.led.BN
    // io.DN_C         := dig.io.led.CN
    // io.DN_D         := dig.io.led.DN
    // io.DN_E         := dig.io.led.EN
    // io.DN_F         := dig.io.led.FN
    // io.DN_G         := dig.io.led.GN
    // io.DN_DP        := dig.io.led.dot

    // /* ---------- 24 个 led 灯 ---------- */

    // val bus2 = bridge.io.devices(2)
    // val reg  = RegInit(VecInit(Seq.fill(3)(0.U(8.W))))
    // for (i <- 0 until 3) {
    //   when(bus2.wen(i)) {
    //     reg(i) := bus2.wdata(8 * i + 7, 8 * i)
    //   }
    // }
    // this.io.led := reg.asUInt

    // /* ---------- 拨码开关 ---------- */

    // val bus3 = bridge.io.devices(3)
    // bus3.rdata := Cat(0.U((32 - 24).W), io.switch)

    // /* ---------- button ---------- */

    // val bus4 = bridge.io.devices(4)
    // bus4.rdata := Cat(0.U((32 - 5).W), this.io.button)

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
