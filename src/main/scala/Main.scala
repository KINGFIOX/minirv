object Hello {
  def main(args: Array[String]) = {
    val x = 12
    val y = 3
    val q = (x + y - 1) / y;
    println(s"Ceil division of $x by $y is $q")
  }
}
