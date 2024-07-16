import chisel3._
import chisel3.util._
import io.blackbox.DistributedSinglePortRAM
import io.blackbox.DistributedSinglePortROM
import common.HasCoreParameter
import io.device.HasSevenSegParameter
import io.blackbox.PLL
import io.blackbox.CLKGen
import core.CPUCore

trait HasSocParameter {
  val addrBits = 14
  def iromLens = 1 << 14 // rom 有 (1 << 14) 个 word
  def dramLens = 1 << 14

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

  val io = FlatIO(new Bundle {
    val fpga_clk = Input(Clock())
    val fpga_rst = Input(Bool())

    /* ---------- 外设 ---------- */

    val switch = Input(UInt(switchBits.W)) // 拨码开关
    val button = Input(UInt(buttonBits.W)) // 中间 5 个按钮
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
    val led = Output(UInt(ledBits.W))

    /* ---------- debug ---------- */

    val debug = new DebugBundle
  })

  private val cpu_clock = CLKGen(this.clock)

  withClock(cpu_clock) {

    val cpu_core = Module(new CPUCore)

    val addr_space_range = Seq(
      (ADDR_MEM_BEGIN, ADDR_MEM_END), // memory
      (ADDR_DIG, ADDR_DIG + digitBytes), //  4 个 Byte
      (ADDR_LED, ADDR_LED + ledBytes), //  24 个 led
      (ADDR_SWITCH, ADDR_SWITCH + ledBytes), // 24 个 switch
      (ADDR_BUTTON, ADDR_BUTTON + buttonBytes) // 5 个 button
    )

  }
}

/** @brief
  *   trace 的调试
  */
class DebugBundle extends Bundle {
  val wb_have_inst = Output(Bool())
  val wb_pc        = Output(UInt(32.W))
  val wb_ena       = Output(Bool())
  val wb_reg       = Output(UInt(5.W))
  val wb_value     = Output(UInt(32.W))
}
