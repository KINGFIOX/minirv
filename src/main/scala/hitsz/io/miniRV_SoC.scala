package hitsz.io

import chisel3._
import chisel3.util._
import hitsz.io.blackbox.DistributedSinglePortRAM
import hitsz.io.blackbox.DistributedSinglePortROM
import hitsz.common.HasCoreParameter
import hitsz.io.device.HasSevenSegParameter
import hitsz.io.blackbox.PLL
import hitsz.io.blackbox.CLKGen
import hitsz.pipeline.CPUCore
import hitsz.io.device.Bridge
import hitsz.io.blackbox.IROM
import hitsz.io.trace.DRAM
import hitsz.io.trace.DebugBundle
import hitsz.io.device.SevenSegDigital
import hitsz.ENV

trait HasSocParameter {

  /* ---------- vivado ---------- */

  val vivado_addrBits = 14
  def vivado_iromLens = 1 << vivado_addrBits // rom 有 (1 << 14) 个 word
  def vivado_dramLens = 1 << vivado_addrBits

  /* ---------- verilator ---------- */

  val verilator_addrBits = 16
  def verilator_iromLens = 1 << verilator_addrBits // rom 有 (1 << 14) 个 word
  def verilator_dramLens = 1 << verilator_addrBits

  /* ---------- ---------- */

  val switchBits = 24
  val buttonBits = 5
  val ledBits    = 24 // 因为有 24 个  led 灯

  def divCeil(x: Int, y: Int): Int = (x + y - 1) / y

  def switchBytes = divCeil(switchBits, 8)
  def buttonBytes = divCeil(buttonBits, 8)
  def ledBytes    = divCeil(ledBits, 8)

  val ADDR_DIG       = 0xffff_f000
  val ADDR_LED       = 0xffff_f060
  val ADDR_SWITCH    = 0xffff_f070
  val ADDR_BUTTON    = 0xffff_f078
  val ADDR_MEM_BEGIN = 0x0000_0000
  val ADDR_MEM_END   = 0xffff_f000
}

class miniRV_SoC extends Module with HasSevenSegParameter with HasSocParameter with HasCoreParameter {

  val io = IO(new Bundle {
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

    val dbg = new DebugBundle
  })

  val cpu_clk = if (ENV.isVivado) CLKGen(this.clock) else this.clock

  withClock(cpu_clk) {

    /* ---------- CPU Core ---------- */

    val cpu_core = Module(new CPUCore)
    io.dbg := cpu_core.io.dbg

    /* ---------- irom ---------- */

    if (ENV.isVivado) {
      val irom = Module(new DistributedSinglePortROM(vivado_iromLens, XLEN))
      irom.io.a             := cpu_core.io.irom.addr(vivado_addrBits, dataBytesBits)
      cpu_core.io.irom.inst := irom.io.spo
    } else {
      val irom = Module(new IROM)
      irom.io.a             := cpu_core.io.irom.addr(verilator_addrBits, dataBytesBits)
      cpu_core.io.irom.inst := irom.io.spo
    }

    // /* ---------- bridge ---------- */

    val addr_space_range = Seq(
      (ADDR_MEM_BEGIN, ADDR_MEM_END) // memory
      // (ADDR_DIG, ADDR_DIG + digitBytes), //  4 个 Byte
      // (ADDR_LED, ADDR_LED + ledBytes), //  24 个 led
      // (ADDR_SWITCH, ADDR_SWITCH + ledBytes) // 24 个 switch
      // (ADDR_BUTTON, ADDR_BUTTON + buttonBytes) // 5 个 button
    )

    val bridge = Module(new Bridge(addr_space_range))
    bridge.io.cpu <> cpu_core.io.bus

    // /* ---------- dram ---------- */

    val bus0 = bridge.io.dev(0)
    if (ENV.isVivado) {
      // vivado
    } else {
      val dram = Module(new DRAM("/home/wangfiox/Documents/minirv/cdp-tests/meminit.bin"))
      dram.io.clk := this.clock
      dram.io.a   := bus0.addr(verilator_addrBits, dataBytesBits) // dram 是 word 寻址的
      dram.io.d   := bus0.wdata
      dram.io.we  := bus0.wen
      bus0.rdata  := dram.io.spo
    }

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
