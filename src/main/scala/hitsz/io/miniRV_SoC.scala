package hitsz.io

import chisel3._
import chisel3.util._
import hitsz.io.vivado.DistributedSinglePortRAM
import hitsz.io.vivado.DistributedSinglePortROM
import hitsz.common.HasCoreParameter
import hitsz.io.device.HasSevenSegParameter
import hitsz.io.vivado.PLL
import hitsz.io.vivado.CLKGen
import hitsz.pipeline.CPUCore
import hitsz.io.device.Bridge
import hitsz.io.verilator.IROM
import hitsz.io.verilator.DRAM
import hitsz.io.verilator.DebugBundle
import hitsz.io.device.SevenSegDigital

trait HasSocParameter {

  /* ---------- vivado ---------- */

  val vivado_addrBits = 14
  def vivado_iromLens = 1 << vivado_addrBits // rom 有 (1 << 14) 个 word
  def vivado_dramLens = 1 << vivado_addrBits

  /* ---------- verilator ---------- */

  val verilator_addrBits = 16
  def verilator_iromLens = 1 << verilator_addrBits // rom 有 (1 << 14) 个 word
  // def verilator_dramLens = 1 << verilator_addrBits
  def verilator_dramLens = verilator_iromLens

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

class miniRV_SoC(isVivado: Boolean, enableDebug: Boolean) extends Module with HasSevenSegParameter with HasSocParameter with HasCoreParameter {

  val io = IO(new Bundle {
    // /* ---------- 外设 ---------- */
    // 8 个数码管
    val dig_en = Output(UInt(digitNum.W))
    val DN_A   = Output(Bool())
    val DN_B   = Output(Bool())
    val DN_C   = Output(Bool())
    val DN_D   = Output(Bool())
    val DN_E   = Output(Bool())
    val DN_F   = Output(Bool())
    val DN_G   = Output(Bool())
    val DN_DP  = Output(Bool())
    //
    val switch = Input(UInt(switchBits.W)) // 拨码开关
    /* ---------- debug ---------- */
    val dbg = if (enableDebug) Some(new DebugBundle) else None
  })

  val cpu_clk = if (isVivado) CLKGen(this.clock) else this.clock

  withClock(cpu_clk) {

    /* ---------- CPU Core ---------- */

    val cpu_core = Module(new CPUCore(enableDebug))
    if (enableDebug) io.dbg.get := cpu_core.io.dbg.get

    /* ---------- irom ---------- */

    if (isVivado) {
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
      (ADDR_MEM_BEGIN, ADDR_MEM_END), // memory
      (ADDR_DIG, ADDR_DIG + digitBytes), //  4 个 Byte -> 数码管
      (ADDR_SWITCH, ADDR_SWITCH + ledBytes) // 24 个 switch
    )

    val bridge = Module(new Bridge(addr_space_range))
    bridge.io.cpu <> cpu_core.io.bus

    // /* ---------- dram ---------- */

    val bus0 = bridge.io.dev(0)
    if (isVivado) {
      // vivado
      val dram = Module(new DistributedSinglePortRAM(vivado_dramLens, XLEN))
      bus0.rdata  := dram.io.spo
      dram.io.clk := cpu_clk
      dram.io.a   := bus0.addr(vivado_addrBits, dataBytesBits)
      dram.io.d   := bus0.wdata
      dram.io.we  := bus0.wen
    } else {
      val dram = Module(new DRAM)
      dram.io.clk := cpu_clk
      dram.io.a   := bus0.addr(verilator_addrBits, dataBytesBits) // dram 是 word 寻址的
      dram.io.d   := bus0.wdata
      dram.io.we  := bus0.wen
      bus0.rdata  := dram.io.spo
    }

    // /* ---------- Seven 数码管 ---------- */

    val bus1 = bridge.io.dev(1)
    if (isVivado) {
      val dig = Module(new SevenSegDigital)
      dig.io.input_en := bus1.wen
      dig.io.input    := bus1.wdata
      bus1.rdata      := DontCare
      io.dig_en       := dig.io.led_enable
      io.DN_A         := dig.io.led.AN
      io.DN_B         := dig.io.led.BN
      io.DN_C         := dig.io.led.CN
      io.DN_D         := dig.io.led.DN
      io.DN_E         := dig.io.led.EN
      io.DN_F         := dig.io.led.FN
      io.DN_G         := dig.io.led.GN
      io.DN_DP        := dig.io.led.dot
    } else {
      bus1.rdata := DontCare
      io.dig_en  := DontCare
      io.DN_A    := DontCare
      io.DN_B    := DontCare
      io.DN_C    := DontCare
      io.DN_D    := DontCare
      io.DN_E    := DontCare
      io.DN_F    := DontCare
      io.DN_G    := DontCare
      io.DN_DP   := DontCare
      printf(p"${bus1.wdata}")
    }

    // /* ---------- 拨码开关 ---------- */

    val bus3 = bridge.io.dev(2)
    if (isVivado) {
      bus3.rdata := io.switch
    } else {
      bus3.rdata := (0x00a0_0000).U
    }

  }
}
