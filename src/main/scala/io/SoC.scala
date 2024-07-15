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

  val ADDR_DIG       = 0xffff_f000
  val ADDR_LED       = 0xffff_f060
  val ADDR_SWITCH    = 0xffff_f070
  val ADDR_BUTTON    = 0xffff_f078
  val ADDR_MEM_BEGIN = 0x0000_0000
  val ADDR_MEM_END   = 0xffff_f000
}

class SoC extends Module with HasSevenSegParameter with HasSocParameter with HasCoreParameter {

  /** @brief
    *   这些是外设了
    */
  val io = IO(new Bundle {
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
  })

  private val cpu_clock = CLKGen(this.clock)

  withClock(cpu_clock) {

    val cpu_core = Module(new CPUCore)

    val addr_space_range = Seq(
      (ADDR_MEM_BEGIN, ADDR_MEM_END), // memory
      (ADDR_DIG, ADDR_DIG + 4), //  4 个 Byte
      (ADDR_LED, ADDR_LED + 3), //  24 个 led
      (ADDR_SWITCH, ADDR_SWITCH + 3), // 24 个 switch
      (ADDR_BUTTON, ADDR_BUTTON + 1) // 5 个 button
    )

  }
}
