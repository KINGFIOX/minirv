import chisel3._
import chisel3.util._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import hitsz.common.Instructions

class GCDSpec extends AnyFreeSpec with Matchers {

  "test BitPat" in {
    simulate(new Module {

      val io = IO(new Bundle {
        val inst = Input(UInt(32.W))
        val out  = Output(Bool())
      })

      io.out := false.B

      when(io.inst === Instructions.ADD) {
        io.out := true.B
        printf("ADD\n")
      }

    }) { dut =>
      val inst = "b00000001111111111000111110110011".U;
      dut.io.inst.poke(inst)
      dut.clock.step()
      dut.io.out.expect(true.B)
    }
  }
}
