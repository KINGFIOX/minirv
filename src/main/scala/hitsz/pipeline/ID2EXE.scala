package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter
import hitsz.component.IF2IDBundle
import hitsz.component.ALUOPBundle
import hitsz.component.WBBundle
import hitsz.component.JMPBundle
import hitsz.component.MemUOpType
import hitsz.component.ALUOpType
import hitsz.component.BRU
import hitsz.component.BRUOpType

class RFRead extends Bundle with HasCoreParameter with HasRegFileParameter {
  val idxes = new Bundle { // 记录 寄存器的 index
    val rs1 = Output(UInt(NRRegbits.W))
    val rs2 = Output(UInt(NRRegbits.W))
    val rd  = Output(UInt(NRRegbits.W))
  }
  val vals = new Bundle { // 记录 读出的寄存器的值
    val rs1 = Output(UInt(XLEN.W))
    val rs2 = Output(UInt(XLEN.W))
  }
}

object RFRead {
  def apply(rs1_i: UInt, rs2_i: UInt, rd_i: UInt, rs1_v: UInt, rs2_v: UInt): RFRead = {
    val rf_read = Wire(Flipped(new RFRead))
    rf_read.idxes.rs1 := rs1_i
    rf_read.idxes.rs2 := rs2_i
    rf_read.idxes.rd  := rd_i
    rf_read.vals.rs1  := rs1_v
    rf_read.vals.rs2  := rs2_v
    rf_read
  }
}

/** @brief
  *   这些都是输出
  */
class ID2EXEBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  // IF
  val pc    = Output(UInt(XLEN.W))
  val valid = Output(Bool()) // if_.valid
  // RF
  val rf = new RFRead
  // CU
  val alu_ctrl = new ALUOPBundle // cu_.imm
  val bru_op   = Output(BRUOpType()) // cu_.imm
  val wb       = new WBBundle // cu_.imm
  val mem      = Output(MemUOpType()) // cu_.imm
  val imm      = Output(UInt(XLEN.W)) // cu_.imm
}

object ID2EXEBundle {
  def apply(pc: UInt, valid: Bool, rf: RFRead, alu_ctrl: ALUOPBundle, bru_op: BRUOpType.Type, wb: WBBundle, mem: MemUOpType.Type, imm: UInt): ID2EXEBundle = {
    val id2exe_l = Wire(Flipped(new ID2EXEBundle))
    id2exe_l.pc       := pc // if_r.pc
    id2exe_l.valid    := valid // if_r.valid
    id2exe_l.rf       := rf // RFRead(rs1_i, rs2_i, rd_i, rs1_v, rs2_v)
    id2exe_l.alu_ctrl := alu_ctrl // cu_.io.alu_ctrl
    id2exe_l.bru_op   := bru_op // cu_.io.bru_op
    id2exe_l.wb       := wb // cu_.io.wb
    id2exe_l.mem      := mem // cu_.io.mem
    id2exe_l.imm      := imm // cu_.io.imm
    id2exe_l
  }
}
