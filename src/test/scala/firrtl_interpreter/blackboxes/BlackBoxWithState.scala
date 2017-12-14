// See LICENSE for license details.

package firrtl_interpreter.blackboxes

import firrtl.ir.Type
import firrtl_interpreter.{BlackBoxFactory, BlackBoxImplementation, InterpreterOptionsManager, InterpretiveTester}
import org.scalatest.{FreeSpec, Matchers}


// scalastyle:off magic.number
class BlackBoxWithState extends FreeSpec with Matchers {
  "BlackBoxWithState should pass a basic test" in {
    val input =
      """
        |circuit AccumBlackBoxWrapper : @[:@2.0]
        |  extmodule AccumBlackBox : @[:@3.2]
        |    input clock : Clock @[:@4.4]
        |    output data : UInt<16> @[:@5.4]
        |
        |    defname = AccumBlackBox
        |
        |
        |  module AccumBlackBoxWrapper : @[:@10.2]
        |    input clock : Clock @[:@11.4]
        |    input reset : UInt<1> @[:@12.4]
        |    output io_data : UInt<16> @[:@13.4]
        |    input io_clock : Clock @[:@13.4]
        |
        |    inst m of AccumBlackBox @[AccumBlackBoxSpec.scala 93:17:@15.4]
        |    node _T_4 = bits(reset, 0, 0) @[AccumBlackBoxSpec.scala 96:9:@20.4]
        |    node _T_6 = eq(_T_4, UInt<1>("h0")) @[AccumBlackBoxSpec.scala 96:9:@21.4]
        |    io_data <= m.data
        |    m.clock <= clock
        |
      """.stripMargin
//    printf(clock, and(and(UInt<1>("h1"), _T_6), UInt<1>("h1")), "m.io.data %d io.data %d\n", m.data, io_data) @[AccumBlackBoxSpec.scala 96:9:@23.6]

    val manager = new InterpreterOptionsManager {
      interpreterOptions = interpreterOptions.copy(blackBoxFactories = Seq(new AccumBlackBoxFactory))
    }
    val tester = new InterpretiveTester(input, manager)

    val initialValue = tester.peek("io_data")
    println(s"Initial value is $initialValue")
    tester.step()
    tester.expect("io_data", initialValue)
    println(s"m.data ${tester.peek("m.data")}")
    tester.step()
    tester.expect("io_data", initialValue + 1)
    println(s"m.data ${tester.peek("m.data")}")
    tester.step()
    tester.expect("io_data", initialValue + 2)
    println(s"m.data ${tester.peek("m.data")}")

    tester.report()
  }
}

/**
  * This is an implementation of a black box whose verilog is contained inline in AccumBlackBox, an instance of this
  * class will be placed into a black box factory so that it can be passed properly to the firrtl interpreter
  * @param name black box name
  */
class AccumFirrtlInterpreterBlackBox( val name : String) extends BlackBoxImplementation {

  var ns : BigInt = 0
  var ps : BigInt = 0

  def outputDependencies(outputName: String): Seq[String] = {
    outputName match {
      case "data" => Seq("clock")
      case _      => Seq.empty
    }
  }

  def cycle(): Unit = {
    ps = ns
    ns = ps + 1
    println(s"blackbox:$name ps $ps ns $ns")
  }

  def execute(inputValues: Seq[BigInt], tpe: Type, outputName: String): BigInt = {
    ps
  }
}

/**
  * The factor that will provide firrtl access to the implementations
  */
class AccumBlackBoxFactory extends BlackBoxFactory {

  def createInstance(instanceName: String, blackBoxName: String): Option[BlackBoxImplementation] = {
    blackBoxName match {
      case "AccumBlackBox" => Some(add(new AccumFirrtlInterpreterBlackBox(instanceName)))
      case _               => None
    }
  }
}