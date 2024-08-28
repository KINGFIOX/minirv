package tests.hitsz.utils

import scala.io.StdIn

class DRAM(image: Array[Byte], val dram_base: Int, val size: Int) {
  private val dram: Array[Byte] = Array.fill(size)(0.toByte)
  Array.copy(image, 0, dram, 0, image.length)

  def load(addr: Int): Int = {
    if (addr == SWITCH_ADDR) {
      StdIn.readLine().toInt
    } else {
      val offset = (addr - dram_base) // dram 的 index
      (dram(offset) & 0x0ff) | ((dram(offset + 1) & 0x0ff) << 8) | ((dram(offset + 2) & 0x0ff) << 16) | ((dram(offset + 3) & 0x0ff) << 24)
    }
  }

  def store(addr: Int, value: Int, wen: Int): Unit = {

    if (wen != 0 && addr == DIG_ADDR) {
      println(s"${BLUE}LED: ${value}${RESET}")
    } else {
      val offset = addr - dram_base // dram 的 index
      if ((wen & 1) != 0) {
        dram(offset) = (value & 0x0ff).toByte
      }
      if ((wen & 2) != 0) {
        dram(offset + 1) = ((value >> 8) & 0x0ff).toByte
      }
      if ((wen & 4) != 0) {
        dram(offset + 2) = ((value >> 16) & 0x0ff).toByte
      }
      if ((wen & 8) != 0) {
        dram(offset + 3) = ((value >> 24) & 0x0ff).toByte
      }
    }
  }

  private val SWITCH_ADDR = 0xffff_f070
  private val DIG_ADDR    = 0xffff_f000

  private val RESET  = "\u001B[0m" // 重置颜色
  private val RED    = "\u001B[31m" // 红色
  private val GREEN  = "\u001B[32m" // 绿色
  private val YELLOW = "\u001B[33m" // 黄色
  private val BLUE   = "\u001B[34m" // 蓝色
  private val PURPLE = "\u001B[35m" // 紫色
  private val CYAN   = "\u001B[36m" // 青色
  private val WHITE  = "\u001B[37m" // 白色
}
