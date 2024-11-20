import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class FanControllerTest extends AnyFlatSpec {
  behavior of "FanController"

  // Helper function to peek the entire matrix and return a flattened Seq of values
  def peekMatrix(matrix: Vec[Vec[UInt]]): Seq[Int] = {
    matrix.flatMap(row => row.map(_.peek().litValue.toInt)).toSeq
  }

  it should "test pattern switching timing in different fan states" in {
    simulate(new FanController) { c =>
      // 初始状态应该是 idle，图案不应切换
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.btn7.poke(false.B)

      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.btn7.poke(false.B)

      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.btn7.poke(false.B)

      val initialPattern = peekMatrix(c.io.matrix) // 记录初始图案

for (i <- 0 until 400) {
  c.clock.step(1) // 等待一个周期
  assert(c.io.disp7.peek().litValue == 2) // idle 的值是 0
  println(s"Cycle $i: ${peekMatrix(c.io.matrix)}") // 打印当前周期的 matrix 值
}

      assert(peekMatrix(c.io.matrix) == initialPattern, "In idle state, pattern should not change")


      // 切换到 low，图案应该每秒切换一次
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.btn7.poke(false.B)
      val lowPattern1 = peekMatrix(c.io.matrix)
      c.clock.step(100) // 1 秒后
      val lowPattern2 = peekMatrix(c.io.matrix)
      assert(lowPattern1 != lowPattern2, "In low state, pattern should change every second")

      // 切换到 medium，图案应该每 0.5 秒切换一次
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.btn7.poke(false.B)
      val mediumPattern1 = peekMatrix(c.io.matrix)
      c.clock.step(50) // 0.5 秒后
      val mediumPattern2 = peekMatrix(c.io.matrix)
      assert(mediumPattern1 != mediumPattern2, "In medium state, pattern should change every 0.5 seconds")

      // 切换到 high，图案应该每 0.25 秒切换一次
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.btn7.poke(false.B)
      val highPattern1 = peekMatrix(c.io.matrix)
      c.clock.step(25) // 0.25 秒后
      val highPattern2 = peekMatrix(c.io.matrix)
      assert(highPattern1 != highPattern2, "In high state, pattern should change every 0.25 seconds")

      // 再次切换到 idle，图案不应再切换
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.btn7.poke(false.B)
      val idlePattern = peekMatrix(c.io.matrix)
      c.clock.step(100) // 等待 1 秒
      assert(peekMatrix(c.io.matrix) == idlePattern, "In idle state, pattern should not change")
    }
  }
}





/*
class FanControllerTest extends AnyFlatSpec {
  behavior of "FanController"

  it should "cycle through fan states with btn7" in {
    simulate(new FanController) { c =>
      // 初始状态应该是 idle
      c.io.disp7.expect(0.U) // idle 的值是 0

      // 按下 btn7，状态应该切换到 low
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.disp7.expect(1.U) // low 的值是 1

      // 再按一次，切换到 medium
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.disp7.expect(2.U) // medium 的值是 2

      // 再按一次，切换到 high
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.disp7.expect(3.U) // high 的值是 3

      // 再按一次，回到 idle
      c.io.btn7.poke(true.B)
      c.clock.step(1)
      c.io.disp7.expect(0.U) // idle 的值是 0
    }
  }
}
*/


