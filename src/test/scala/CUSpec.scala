import chisel3._
import chisel3.util._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import hitsz.common.Instructions
import hitsz.io.miniRV_SoC

import chisel3.testers.BasicTester

import _root_.rv32i.RVEMU
import rv32i.Settings
import rv32i.Main.readFile

class GCDSpec extends AnyFreeSpec with Matchers {

  val user: Array[Byte] = readFile("meminit.bin") match {
    case Right(bytes) => bytes
    case Left(error) => Array[Byte]()
  }

  val kernel = readFile("trap_handle.bin") match {
    case Right(bytes) => bytes
    case Left(error) => Array[Byte]()
  }

  val emu = new RVEMU(user, kernel)

  "test BitPat" in {
    simulate(new miniRV_SoC) { dut =>
      // 初始化
      dut.io.fpga_rst.poke(true.B)
      dut.io.fpga_clk.step(10)
      dut.io.fpga_rst.poke(false.B)

      for (_ <- 0 until 10) {
        val dbg = dut.io.dbg
        emu.step()
        dut.io.fpga_clk.step()
        println(s"${dbg}")
      }
    }
  }
}
