package rv32i

import java.io.{File, FileInputStream, IOException}
import scala.io.Source
import scala.util.{Try, Using}

object Settings {
  val DRAM_BEGIN: Int = 0 // 0x0_8000_0000
  val DRAM_LEN: Int   = (1 << 16)

  val USER_BEGIN: Int = 0

  val KERNEL_BEGIN: Int = 0x0_1c09_0000

  val DIG_BEGIN: Int = 0x0_ffff_f000 //
  val DIG_LEN: Int   = 4

  val LED_BEGIN: Int = 0x0_ffff_f060
  val LED_LEN: Int   = 3

  val SWITCH_BEGIN: Int = 0x0_ffff_f070
  val SWITCH_LEN: Int   = 3

  val BTN_BEGIN: Int = 0x0_ffff_f078
  val BTN_LEN: Int   = 1
}

object Main {
  def main(args: Array[String]): Unit = {

    val user = readFile("meminit.bin") match {
      case Right(bytes) => bytes
      case Left(error) =>
        println(s"Error reading file: $error")
        return
    }

    val kernel = readFile("trap_handle.bin") match {
      case Right(bytes) => bytes
      case Left(error) =>
        println(s"Error reading file: $error")
        return
    }

    val cpu = new RVEMU(user, kernel)

    println(s"sp=${cpu.regs(2)}")

    while (cpu.step()._1) {
      // // 1. Fetch.
      // val inst = cpu.fetch()

      // // Break the loop if an error occurs.
      // if (inst == 0) return

      // println(s"========== dump ==========")

      // // 打印当前 pc 的值
      // println(s"cur_pc=${java.lang.Integer.toHexString(cpu.pc)}")

      // // 2. Add 4 to the program counter.
      // // 当然, cpu.execute 可能会改掉这个 pc 值
      // cpu.pc += 4

      // // 3. Decode.
      // // 4. Execute.
      // if (cpu.execute(inst) == false) return

      // // This is a workaround for avoiding an infinite loop.
      // if (cpu.pc == 0) return

      // // 打印执行后的状态
      // cpu.dumpRegisters()
    }

  }

  def readFile(filename: String): Either[Throwable, Array[Byte]] = {
    Using(new FileInputStream(new File(filename))) { inputStream =>
      val buffer = new Array[Byte](inputStream.available())
      inputStream.read(buffer)
      buffer
    }.toEither
  }
}
