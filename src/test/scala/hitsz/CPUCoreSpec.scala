package test.hitsz

import chisel3._
import chisel3.util._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import hitsz.pipeline.CPUCore
import test.hitsz.utils.EphemeralSimulator._
import hitsz.io.HasSocParameter

import java.io.{File, FileInputStream, IOException}
import scala.io.Source
import scala.util.{Try, Using}
import java.nio.file.Files
import test.hitsz.utils.IROM
import tests.hitsz.utils.DRAM

class CPUCoreSpec extends AnyFreeSpec with Matchers with HasSocParameter {

  def abi(reg_i: Int) = {
    val _abi = Seq(
      "zero",
      "ra",
      "sp",
      "gp",
      "tp",
      "t0",
      "t1",
      "t2",
      "s0",
      "s1",
      "a0",
      "a1",
      "a2",
      "a3",
      "a4",
      "a5",
      "a6",
      "a7",
      "s2",
      "s3",
      "s4",
      "s5",
      "s6",
      "s7",
      "s8",
      "s9",
      "s10",
      "s11",
      "t3",
      "t4",
      "t5",
      "t6"
    )
    if (reg_i < 0 || reg_i >= _abi.length) {
      reg_i match {
        case _ => "unknown"
      }
    } else {
      _abi(reg_i)
    }
  }

  def csr_abi(csr_addr: Int): String = {
    csr_addr match {
      case 0x300 => "mstatus"
      case 0x341 => "mepc"
      case 0x342 => "mcause"
      case _ => java.lang.Integer.toHexString(csr_addr)
    }
  }

  def disasm(inst: Int): String = {
    val opcode = inst & 0x7f
    val rd     = (inst >>> 7) & 0x1f
    val rs1    = (inst >>> 15) & 0x1f
    val rs2    = (inst >>> 20) & 0x1f
    val funct3 = (inst >>> 12) & 0x7
    val funct7 = (inst >>> 25) & 0x7f

    opcode match {
      case 0x03 => // load
        val imm = inst >> 20 /* 这个是有符号的 */
        funct3 match {
          case 0x0 => s"lb ${abi(rd)}, $imm(${abi(rs1)})"
          case 0x1 => s"lh ${abi(rd)}, $imm(${abi(rs1)})"
          case 0x2 => s"lw ${abi(rd)}, $imm(${abi(rs1)})"
          case 0x4 => s"lbu ${abi(rd)}, $imm(${abi(rs1)})"
          case 0x5 => s"lhu ${abi(rd)}, $imm(${abi(rs1)})"
          case _ => ""
        }
      case 0x23 => // 0b010_0011 -> store
        val imm = ((inst >> 25) << 5) | ((inst >> 7) & 0x1f)
        funct3 match {
          case 0x0 => s"sb ${abi(rs2)}, $imm(${abi(rs1)})"
          case 0x1 => s"sh ${abi(rs2)}, $imm(${abi(rs1)})"
          case 0x2 => s"sw ${abi(rs2)}, $imm(${abi(rs1)})"
          case _ => ""
        }
      case 0x13 => // alu imm
        val imm   = inst >> 20
        val shamt = imm & 0x3f // 0b0011_1111
        funct3 match {
          case 0x0 => s"addi ${abi(rd)}, ${abi(rs1)}, $imm"
          case 0x1 => s"slli ${abi(rd)}, ${abi(rs1)}, $shamt"
          case 0x2 => s"slti ${abi(rd)}, ${abi(rs1)}, $imm"
          case 0x3 => s"sltiu ${abi(rd)}, ${abi(rs1)}, $imm"
          case 0x4 => s"xori ${abi(rd)}, ${abi(rs1)}, $imm"
          case 0x6 => s"ori ${abi(rd)}, ${abi(rs1)}, $imm"
          case 0x7 => s"andi ${abi(rd)}, ${abi(rs1)}, $imm"
          case 0x5 =>
            if ((funct7 >> 1) == 0x00) s"srli ${abi(rd)}, ${abi(rs1)}, $shamt"
            else s"srai ${abi(rd)}, ${abi(rs1)}, $shamt"
          case _ => ""
        }
      case 0x17 => // 0b001_0111 -> auipc
        val imm = (inst & 0xfffff000) >> 12
        s"auipc ${abi(rd)}, $imm"
      case 0x33 => // 0b011_0011 -> alu
        (funct3, funct7) match {
          case (0x0, 0x00) => s"add ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x0, 0x20) => s"sub ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x1, 0x00) => s"sll ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x2, 0x00) => s"slt ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x3, 0x00) => s"sltu ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x4, 0x00) => s"xor ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x5, 0x00) => s"srl ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x5, 0x20) => s"sra ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x6, 0x00) => s"or ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case (0x7, 0x00) => s"and ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}"
          case _ => ""
        }
      case 0x37 => s"lui ${abi(rd)}, ${inst & 0x0_ffff_f000}"
      case 0x63 => // branch
        // val imm = (((inst >> 31) << 12) | ((inst >> 25) << 5) | ((inst >> 8) & 0x0f) | ((inst >> 7) & 0x01)) << 1
        val imm = ((inst & 0x0_8000_0000) >> 19) | ((inst & 0x0_80) << 4) | ((inst >> 20) & 0x0_7e0) | ((inst >> 7) & 0x0_1e)
        funct3 match {
          case 0x0 => s"beq ${abi(rs1)}, ${abi(rs2)}, $imm"
          case 0x1 => s"bne ${abi(rs1)}, ${abi(rs2)}, $imm"
          case 0x4 => s"blt ${abi(rs1)}, ${abi(rs2)}, $imm"
          case 0x5 => s"bge ${abi(rs1)}, ${abi(rs2)}, $imm"
          case 0x6 => s"bltu ${abi(rs1)}, ${abi(rs2)}, $imm"
          case 0x7 => s"bgeu ${abi(rs1)}, ${abi(rs2)}, $imm"
          case _ => ""
        }
      case 0x67 => // 0b110_0111 -> jalr
        val imm = inst >> 20
        s"jalr ${abi(rd)}, ${abi(rs1)}, $imm"
      case 0x6f => // 0b110_1111 -> jal
        // val imm = (((inst >> 31) << 20) | ((inst >> 12) & 0xff) | ((inst >> 20) & 0x01) | ((inst >> 21) & 0x3ff))
        val imm = ((inst & 0x0_8000_0000) >> 11) | (inst & 0x0_f_f000) | ((inst >> 9) & 0x0_800) | ((inst >> 20) & 0x0_7fe)
        s"jal ${abi(rd)}, $imm"
      case 0x73 => // 0b111_0011 -> system
        val csr_addr = inst >>> 20
        funct3 match {
          case 0x0 =>
            funct7 match {
              case 0 => // ecall 或者是 uret
                rs2 match {
                  case 0 => // ecall
                    "ecall"
                  case 0x02 /* uret */ =>
                    "eret"
                }
              case 0x18 /* mret */ | 0x08 /* sret */ =>
                "eret"
            }
          case 0x1 => /* csrrw */ s"csrrw ${abi(rd)}, ${csr_abi(csr_addr)} ,${abi(rs1)}"
          case 0x2 => /* csrrs */ s"csrrs ${abi(rd)}, ${csr_abi(csr_addr)} ,${abi(rs1)}"
          case 0x3 => /* csrrc */ s"csrrc ${abi(rd)}, ${csr_abi(csr_addr)} ,${abi(rs1)}"
          case 0x5 => /* csrrwi */ s"csrrwi ${abi(rd)}, ${csr_abi(csr_addr)} ,${rs1}"
          case 0x6 => /* csrrsi */ s"csrrsi ${abi(rd)}, ${csr_abi(csr_addr)} ,${rs1}"
          case 0x7 => /* csrrci */ s"csrrci ${abi(rd)}, ${csr_abi(csr_addr)} ,${rs1}"
          case _ => ""
        }
      case _ =>
        return ""
    }
  }

  "test cpu core" in {
    simulate(new CPUCore(enableDebug = true)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      val user: Array[Byte] = Files.readAllBytes(new File("random.bin").toPath)
      val irom              = new IROM(user, 0)
      val dram              = new DRAM(user, 0, (1 << 14) << 2)

      // var cycles: Int = 0
      // while (true) {

      //   val pc: Int   = dut.io.irom.addr.peek().litValue.toInt
      //   val inst: Int = irom.fetch(pc)
      //   dut.io.irom.inst.poke(inst)
      //   val asm = disasm(inst)
      //   println(asm)

      //   val addr: Int  = dut.io.bus.addr.peek().litValue.toInt
      //   val rdata: Int = dram.load(addr)
      //   dut.io.bus.rdata.poke(rdata)
      //   val wen   = dut.io.bus.wen.peek().litValue.toInt
      //   val wdata = dut.io.bus.wdata.peek().litValue.toInt
      //   dram.store(addr, wdata, wen)

      //   // Step the simulation forward.
      //   dut.clock.step()
      //   cycles += 1
      // }

    }
  }

}
