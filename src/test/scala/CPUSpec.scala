import chisel3._
import chisel3.util._
import chisel3.simulator.EphemeralSimulator._
import chisel3.testers._

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import hitsz.common.Instructions
import hitsz.io.miniRV_SoC

import rv32i.RVEMU
import rv32i.Settings
import rv32i.Main.readFile

class CPUSpec extends AnyFreeSpec with Matchers {

  val emu = new RVEMU(
    readFile("meminit.bin") match {
      case Right(bytes) => bytes
      case Left(error) => Array[Byte]()
    },
    readFile("trap_handle.bin") match {
      case Right(bytes) => bytes
      case Left(error) => Array[Byte]()
    }
  )

  "test BitPat" in {
    simulate(new miniRV_SoC) { dut =>
      // 初始化

    }
  }
}
