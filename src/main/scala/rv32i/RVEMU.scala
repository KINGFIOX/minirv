package rv32i

class RVEMU(user: Array[Byte], kernel: Array[Byte]) {

  /* ---------- 属性 ---------- */

  val regs: Array[Int] = Array.fill(32)(0) // 我自己约束是 u32

  val csrs: Array[Int] = Array.fill(4096)(0) // 有 4096 个 csr

  var pc: Int = 0 // 地址

  regs(2) = Settings.DRAM_BEGIN + Settings.DRAM_LEN // 设置 sp 为栈顶(高地址)

  val bus = new BUS(user)

  val irom = new IROM(user, Settings.USER_BEGIN, kernel, Settings.KERNEL_BEGIN)

  /* ---------- 方法 ---------- */

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

  // 打印所有寄存器的值
  def dumpRegisters(): Unit = {
    for (i <- 0 until 32 by 4) {
      println(s"${abi(i)}=${regs(i)}\t${abi(i + 1)}=${regs(i + 1)}\t${abi(i + 2)}=${regs(i + 2)}\t${abi(i + 3)}=${regs(i + 3)}\t")
    }
  }

  def load(addr: Int, size: Int): Int = {
    bus.load(addr, size)
  }

  def store(addr: Int, size: Int, value: Int): Unit = {
    bus.store(addr, size, value)
  }

  def fetch(): Int = {
    irom.fetch(pc)
  }

  /** @param inst
    * @return
    *   true : 成功, false : 失败
    */
  def execute(inst: Int): Boolean = {
    val opcode = inst & 0x7f
    val rd     = (inst >>> 7) & 0x0_1f
    val rs1    = (inst >>> 15) & 0x0_1f
    val rs2    = (inst >>> 20) & 0x0_1f
    val funct3 = (inst >>> 12) & 0x0_7
    val funct7 = (inst >>> 25) & 0x0_7f

    regs(0) = 0 // 开始前清 0

    opcode match {
      case 0x03 => // load
        val imm  = inst >> 20 /* 这个是有符号的 */
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 => regs(rd) = load(addr, 8).toByte
          case 0x1 => regs(rd) = load(addr, 16).toShort
          case 0x2 => regs(rd) = load(addr, 32)
          case 0x4 => regs(rd) = load(addr, 8)
          case 0x5 => regs(rd) = load(addr, 16)
          case _ => return false
        }
      case 0x23 => // 0b010_0011 -> store
        val imm  = ((inst >> 25) << 5) | ((inst >> 7) & 0x1f)
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 => store(addr, 8, regs(rs2))
          case 0x1 => store(addr, 16, regs(rs2))
          case 0x2 => store(addr, 32, regs(rs2))
          case _ => return false
        }
      case 0x13 => // alu imm
        val imm   = inst >> 20
        val shamt = imm & 0x3f // 0b0011_1111
        funct3 match {
          case 0x0 => regs(rd) = regs(rs1) + imm
          case 0x1 => regs(rd) = regs(rs1) << shamt
          case 0x2 => regs(rd) = if (regs(rs1) < imm) 1 else 0 // slt
          case 0x3 => regs(rd) = if ((regs(rs1).toLong & 0x0_ffff_ffffL) < (imm.toLong & 0x0_ffff_ffffL)) 1 else 0 // sltu
          case 0x4 => regs(rd) = regs(rs1) ^ imm
          case 0x6 => regs(rd) = regs(rs1) | imm
          case 0x7 => regs(rd) = regs(rs1) & imm
          case 0x5 =>
            if ((funct7 >> 1) == 0x00) { regs(rd) = regs(rs1) >>> shamt }
            else { regs(rd) = (regs(rs1) >> shamt) }
          case _ => return false
        }
      case 0x17 => // 0b001_0111 -> auipc
        val imm = inst & 0xfffff000
        regs(rd) = pc - 4 + imm
      case 0x33 => // 0b011_0011 -> alu
        val shamt = regs(rs2) & 0x3f
        (funct3, funct7) match {
          case (0x0, 0x00) => regs(rd) = regs(rs1) + regs(rs2)
          case (0x0, 0x20) => regs(rd) = regs(rs1) - regs(rs2)
          case (0x1, 0x00) => regs(rd) = regs(rs1) << shamt
          case (0x2, 0x00) => regs(rd) = if (regs(rs1) < regs(rs2)) 1 else 0 // slt
          case (0x3, 0x00) => regs(rd) = if ((regs(rs1).toLong & 0x0_ffff_ffffL) < (regs(rs2).toLong & 0x0_ffff_ffffL)) 1 else 0 // sltu
          case (0x4, 0x00) => regs(rd) = regs(rs1) ^ regs(rs2)
          case (0x5, 0x00) => regs(rd) = regs(rs1) >>> shamt
          case (0x5, 0x20) => regs(rd) = (regs(rs1) >> shamt)
          case (0x6, 0x00) => regs(rd) = regs(rs1) | regs(rs2)
          case (0x7, 0x00) => regs(rd) = regs(rs1) & regs(rs2)
          case _ => return false
        }
      case 0x37 =>
        regs(rd) = inst & 0x0_ffff_f000 // 0b011_0111 -> lui
      case 0x63 => // branch
        // val imm = (((inst >> 31) << 12) | ((inst >> 25) << 5) | ((inst >> 8) & 0x0f) | ((inst >> 7) & 0x01)) << 1
        val imm = ((inst & 0x0_8000_0000) >> 19) | ((inst & 0x0_80) << 4) | ((inst >> 20) & 0x0_7e0) | ((inst >> 7) & 0x0_1e)
        funct3 match {
          case 0x0 => if (regs(rs1) == regs(rs2)) pc += imm - 4
          case 0x1 => if (regs(rs1) != regs(rs2)) pc += imm - 4
          case 0x4 => if (regs(rs1) < regs(rs2)) pc += imm - 4
          case 0x5 => if (regs(rs1) >= regs(rs2)) pc += imm - 4
          case 0x6 => if (regs(rs1) < regs(rs2)) pc += imm - 4
          case 0x7 => if (regs(rs1) >= regs(rs2)) pc += imm - 4
          case _ => return false
        }
      case 0x67 => // 0b110_0111 -> jalr
        val t   = pc
        val imm = inst >> 20
        pc       = (regs(rs1) + imm) & ~1
        regs(rd) = t
      case 0x6f => // 0b110_1111 -> jal
        // val imm = (((inst >> 31) << 20) | ((inst >> 12) & 0xff) | ((inst >> 20) & 0x01) | ((inst >> 21) & 0x3ff))
        regs(rd) = pc
        val imm = ((inst & 0x0_8000_0000) >> 11) | (inst & 0x0_f_f000) | ((inst >> 9) & 0x0_800) | ((inst >> 20) & 0x0_7fe)
        pc += imm - 4
      case 0x73 => // 0b111_0011 -> system
        val csr_addr = inst >>> 20
        funct3 match {
          case 0x0 =>
            funct7 match {
              case 0 => // ecall 或者是 uret
                rs2 match {
                  case 0 => // ecall
                    csrs(0x341) = pc // mepc
                    csrs(0x342) = 0x0000000b // mstatus
                    pc          = Settings.KERNEL_BEGIN
                  case 0x02 /* uret */ =>
                    pc          = csrs(0x341) // mepc
                    csrs(0x342) = 0 // 清空 mcause
                }
              case 0x18 /* mret */ | 0x08 /* sret */ =>
                pc          = csrs(0x341) // mepc
                csrs(0x342) = 0 // 清空 mcause
            }
          case 0x1 => // csrrw
            val t = csrs(csr_addr)
            csrs(csr_addr) = regs(rs1)
            regs(rd)       = t
          case 0x2 => // csrrs
            val t = csrs(csr_addr)
            csrs(csr_addr) = t | regs(rs1)
            regs(rd)       = t
          case 0x3 => // csrrc
            val t = csrs(csr_addr)
            csrs(csr_addr) = t & ~regs(rs1)
            regs(rd)       = t
          case 0x5 => // csrrwi
            val t = csrs(csr_addr)
            csrs(csr_addr) = rs1
            regs(rd)       = t
          case 0x6 => // csrrsi
            val t = csrs(csr_addr)
            csrs(csr_addr) = t | rs1
            regs(rd)       = t
          case 0x7 => // csrrci
            val t = csrs(csr_addr)
            csrs(csr_addr) = t & ~rs1
            regs(rd)       = t
          case _ => return false
        }
      case _ =>
        return false
    }

    regs(0) = 0 // 结束时清零

    true
  }

  /** @brief
    *   可以放在 exe 之前
    *
    * @param inst
    * @return
    */
  def print_inst(inst: Int): Boolean = {
    val opcode = inst & 0x7f
    val rd     = (inst >>> 7) & 0x1f
    val rs1    = (inst >>> 15) & 0x1f
    val rs2    = (inst >>> 20) & 0x1f
    val funct3 = (inst >>> 12) & 0x7
    val funct7 = (inst >>> 25) & 0x7f

    opcode match {
      case 0x03 => // load
        val imm  = inst >> 20 /* 这个是有符号的 */
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 => println(s"lb ${abi(rd)}, $imm(${abi(rs1)})")
          case 0x1 => println(s"lh ${abi(rd)}, $imm(${abi(rs1)})")
          case 0x2 => println(s"lw ${abi(rd)}, $imm(${abi(rs1)})")
          case 0x4 => println(s"lbu ${abi(rd)}, $imm(${abi(rs1)})")
          case 0x5 => println(s"lhu ${abi(rd)}, $imm(${abi(rs1)})")
          case _ => return false
        }
      case 0x23 => // 0b010_0011 -> store
        val imm  = ((inst >> 25) << 5) | ((inst >> 7) & 0x1f)
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 => println(s"sb ${abi(rs2)}, $imm(${abi(rs1)})")
          case 0x1 => println(s"sh ${abi(rs2)}, $imm(${abi(rs1)})")
          case 0x2 => println(s"sw ${abi(rs2)}, $imm(${abi(rs1)})")
          case _ => return false
        }
      case 0x13 => // alu imm
        val imm   = inst >> 20
        val shamt = imm & 0x3f // 0b0011_1111
        funct3 match {
          case 0x0 => println(s"addi ${abi(rd)}, ${abi(rs1)}, $imm")
          case 0x1 => println(s"slli ${abi(rd)}, ${abi(rs1)}, $shamt")
          case 0x2 => println(s"slti ${abi(rd)}, ${abi(rs1)}, $imm")
          case 0x3 => println(s"sltiu ${abi(rd)}, ${abi(rs1)}, $imm")
          case 0x4 => println(s"xori ${abi(rd)}, ${abi(rs1)}, $imm")
          case 0x6 => println(s"ori ${abi(rd)}, ${abi(rs1)}, $imm")
          case 0x7 => println(s"andi ${abi(rd)}, ${abi(rs1)}, $imm")
          case 0x5 =>
            if ((funct7 >> 1) == 0x00) { println(s"srli ${abi(rd)}, ${abi(rs1)}, $shamt") }
            else { println(s"srai ${abi(rd)}, ${abi(rs1)}, $shamt") }
          case _ => return false
        }
      case 0x17 => // 0b001_0111 -> auipc
        val imm = (inst & 0xfffff000) >> 12
        println(s"auipc ${abi(rd)}, $imm")
      case 0x33 => // 0b011_0011 -> alu
        val shamt = regs(rs2) & 0x3f
        (funct3, funct7) match {
          case (0x0, 0x00) => println(s"add ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x0, 0x20) => println(s"sub ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x1, 0x00) => println(s"sll ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x2, 0x00) => println(s"slt ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x3, 0x00) => println(s"sltu ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x4, 0x00) => println(s"xor ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x5, 0x00) => println(s"srl ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x5, 0x20) => println(s"sra ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x6, 0x00) => println(s"or ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case (0x7, 0x00) => println(s"and ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
          case _ => return false
        }
      case 0x37 => println(s"lui ${abi(rd)}, ${inst & 0x0_ffff_f000}")
      case 0x63 => // branch
        // val imm = (((inst >> 31) << 12) | ((inst >> 25) << 5) | ((inst >> 8) & 0x0f) | ((inst >> 7) & 0x01)) << 1
        val imm = ((inst & 0x0_8000_0000) >> 19) | ((inst & 0x0_80) << 4) | ((inst >> 20) & 0x0_7e0) | ((inst >> 7) & 0x0_1e)
        funct3 match {
          case 0x0 => println(s"beq ${abi(rs1)}, ${abi(rs2)}, $imm")
          case 0x1 => println(s"bne ${abi(rs1)}, ${abi(rs2)}, $imm")
          case 0x4 => println(s"blt ${abi(rs1)}, ${abi(rs2)}, $imm")
          case 0x5 => println(s"bge ${abi(rs1)}, ${abi(rs2)}, $imm")
          case 0x6 => println(s"bltu ${abi(rs1)}, ${abi(rs2)}, $imm")
          case 0x7 => println(s"bgeu ${abi(rs1)}, ${abi(rs2)}, $imm")
          case _ => return false
        }
      case 0x67 => // 0b110_0111 -> jalr
        val imm = inst >> 20
        println(s"jalr ${abi(rd)}, ${abi(rs1)}, $imm")
      case 0x6f => // 0b110_1111 -> jal
        // val imm = (((inst >> 31) << 20) | ((inst >> 12) & 0xff) | ((inst >> 20) & 0x01) | ((inst >> 21) & 0x3ff))
        val imm = ((inst & 0x0_8000_0000) >> 11) | (inst & 0x0_f_f000) | ((inst >> 9) & 0x0_800) | ((inst >> 20) & 0x0_7fe)
        println(s"jal ${abi(rd)}, $imm")
      case 0x73 => // 0b111_0011 -> system
        val csr_addr = inst >>> 20
        funct3 match {
          case 0x0 =>
            funct7 match {
              case 0 => // ecall 或者是 uret
                rs2 match {
                  case 0 => // ecall
                    println(s"ecall")
                  case 0x02 /* uret */ =>
                    println(s"eret")
                }
              case 0x18 /* mret */ | 0x08 /* sret */ =>
                println(s"eret")
            }
          case 0x1 => /* csrrw */ println(s"csrrw ${abi(rd)}, ${csr_abi(csr_addr)} ,${abi(rs1)}")
          case 0x2 => /* csrrs */ println(s"csrrs ${abi(rd)}, ${csr_abi(csr_addr)} ,${abi(rs1)}")
          case 0x3 => /* csrrc */ println(s"csrrc ${abi(rd)}, ${csr_abi(csr_addr)} ,${abi(rs1)}")
          case 0x5 => /* csrrwi */ println(s"csrrwi ${abi(rd)}, ${csr_abi(csr_addr)} ,${rs1}")
          case 0x6 => /* csrrsi */ println(s"csrrsi ${abi(rd)}, ${csr_abi(csr_addr)} ,${rs1}")
          case 0x7 => /* csrrci */ println(s"csrrci ${abi(rd)}, ${csr_abi(csr_addr)} ,${rs1}")
          case _ => return false
        }
      case _ =>
        return false
    }

    true
  }

  def wb_status(inst: Int): (Boolean /* 写使能 */, String /* 写了哪个寄存器 */, Int /* 写的寄存器的值 */ ) = {
    val opcode = inst & 0x7f
    val rd     = (inst >>> 7) & 0x1f
    val rs1    = (inst >>> 15) & 0x1f
    val rs2    = (inst >>> 20) & 0x1f
    val funct3 = (inst >>> 12) & 0x7
    val funct7 = (inst >>> 25) & 0x7f

    opcode match {
      case 0x03 => // load
        val imm  = inst >> 20 /* 这个是有符号的 */
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 /* lb */ | 0x1 /* lh */ | 0x2 /* lw */ | 0x4 /* lbu */ | 0x5 /* lhu */ =>
          case _ => return (false, "", 0)
        }
      case 0x13 => // alu imm
        funct3 match {
          case 0x0 /* addi */ | 0x1 /* slli */ | 0x2 /* slti */ | 0x3 /* sltiu */ | 0x4 /* xori */ | 0x6 /* ori */ | 0x7 /* andi */ | 0x5 /* srai, srli */ =>
          case _ => return (false, "", 0)
        }
      case 0x17 => // 0b001_0111 -> auipc
      case 0x33 => // 0b011_0011 -> alu
        (funct3, funct7) match {
          case (0x0, 0x00) | (0x0, 0x20) | (0x1, 0x00) | (0x2, 0x00) | (0x3, 0x00) | (0x4, 0x00) | (0x5, 0x00) | (0x5, 0x20) | (0x6, 0x00) | (0x7, 0x00) =>
          case _ => return (false, "", 0)
        }
      case 0x37 => /* lui */
      case 0x67 => /* jalr */
      case 0x6f => /* jal */
      case _ => return (false, "", 0)
    }

    return (true, abi(rd), regs(rd))
  }

  def step(): (Boolean, (Boolean, String /* 写了哪个寄存器 */, Int /* 写的寄存器的值 */ )) = {
    println(s"========== ${java.lang.Integer.toHexString(pc)} ==========")
    val inst = fetch() // 1. 取指令
    if (!print_inst(inst)) println(f"not implemented yet: $inst%#x")
    if (inst == 0) return (false, (false, "", 0))
    pc += 4
    if (execute(inst) == false) return (false, (false, "", 0))
    if (pc == 0) return (false, (false, "", 0))
    dumpRegisters() // 打印执行后的状态
    return (true, wb_status(inst))
  }

}
