import chisel3._
import chisel3.util._
import io.blackbox.DistributedSinglePortRAM
import io.blackbox.DistributedSinglePortROM
import common.HasCoreParameter
import io.device.HasSevenSegParameter
import io.blackbox.PLL
import io.blackbox.CLKGen

trait HasSocParameter {
  val addrBits = 14
  def iromLens = 1 << 14 // rom 有 (1 << 14) 个 word
  def dramLens = 1 << 14

  val switchBits = 24
  val buttonBits = 5

  val ledBits = 24 // 因为有 24 个  led 灯
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

  withClock(cpu_clock) {}
}
