//> using scala "2.13.12"
//> using dep "org.chipsalliance::chisel:6.5.0"
//> using plugin "org.chipsalliance:::chisel-plugin:6.5.0"
//> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"

import chisel3._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import chisel3.util._ // 确保导入这个包，包含了 Cat 函数

// 使用 Chisel 的 Enum
object FanStates extends ChiselEnum {
  val idle, low, medium, high = Value
}

class FanController extends Module {
  val io = IO(new Bundle {
    val btn7 = Input(Bool())    // 风扇挡位切换按键
    val sw0 = Input(Bool())     // 充电开关
    val disp7 = Output(UInt(4.W)) // 挡位显示
    val disp1 = Output(UInt(8.W)) // 电量显示高位
    val disp0 = Output(UInt(8.W)) // 电量显示低位
    val ld0 = Output(Bool())    // LED指示灯
    val matrix = Output(Vec(8, Vec(8, UInt(8.W)))) // 双色点阵输出
  })

  // 挡位状态
  import FanStates._
  val state = RegInit(idle)

  // 电量计数器（最大99）
  val maxBattery = 99.U
  val battery = RegInit(maxBattery)

  // 计时器
  val timer0 = RegInit(0.U(32.W))
  val timeLimit = WireDefault(100.U) // clock cycle: 0.01s
  switch(state) {
    is(low)    { timeLimit := 100.U } // 低速
    is(medium) { timeLimit := 50.U }  // 中速
    is(high)   { timeLimit := 25.U }  // 高速
  }

  // 图案索引和切换
  val patternIndex = RegInit(0.U(2.W))
  when(timer0 === 0.U) {
    timer0 := timeLimit
    patternIndex := patternIndex + 1.U
  }.otherwise {
    timer0 := timer0 - 1.U
  }

  when(io.btn7) {
    switch(state) {
      is(idle)   { state := low }
      is(low)    { state := medium }
      is(medium) { state := high }
      is(high)   { state := idle }
    }
  }


  // 点阵图案定义
  val patterns = VecInit(
    VecInit("b00011000".U, "b00100100".U, "b01000010".U, "b10000001".U,
            "b01000010".U, "b00100100".U, "b00011000".U, "b00000000".U),
    VecInit("b00000000".U, "b00111100".U, "b01000010".U, "b10000001".U,
            "b01000010".U, "b00111100".U, "b00000000".U, "b00000000".U),
    VecInit("b11111111".U, "b10000001".U, "b10111101".U, "b10000001".U,
            "b10111101".U, "b10000001".U, "b11111111".U, "b00000000".U),
    VecInit("b00011000".U, "b00111100".U, "b01111110".U, "b11111111".U,
            "b01111110".U, "b00111100".U, "b00011000".U, "b00000000".U)
  )

  // io.matrix := patterns(patternIndex)
  io.matrix := VecInit(Seq.fill(8)(patterns(patternIndex)))


val isCharging = Wire(Bool())
isCharging := io.sw0 // 充电开关控制
val timer1 = RegInit(0.U(32.W))

// 电量管理逻辑
when(isCharging) {
  // 充电模式
  when(battery < maxBattery) {
    // 使用 Mux 进行硬件条件选择
    val chargeRate = Mux(state === idle, 20.U, 10.U)
    when(timer1 === 0.U) {
      timer1 := chargeRate
      battery := battery + 1.U
    }.otherwise {
      timer1 := timer1 - 1.U
    }
  }
}.otherwise {
  // 耗电模式
  when(state =/= idle && battery > 0.U) {
    when(timer1 === 0.U) {
      timer1 := 20.U // 0.2 秒的耗电周期
      battery := battery - 1.U
    }.otherwise {
      timer1 := timer1 - 1.U
    }
  }
}

  // LED状态指示
  io.ld0 := Mux(battery <= 25.U, (timer1 % 50000000.U) < 25000000.U, true.B)

  // 数码管显示
  io.disp7 := state.asUInt


  val timer2 = RegInit(0.U(32.W))
  when(battery === 0.U) {
  // 电量为0的情况，显示保持2秒然后熄灭
  when(timer2 === 0.U) {
    // 控制显示关闭
    timer1 := 200.U
    io.disp0 := 0.U
    io.disp1 := 0.U
  }.otherwise {
    io.disp0 := 0.U // 显示 "00"
    io.disp1 := 0.U
    timer2 := timer2 - 1.U
  }
  }.otherwise {
  // 根据电量值计算数码管显示的值
  io.disp1 := battery / 10.U
  io.disp0 := battery % 10.U
  } 
}


object Main extends App {
  println(
    ChiselStage.emitSystemVerilog(
      new FanController,
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
  )
}