// See LICENSE for license details.

package firrtl_interpreter.executable

import firrtl.WireKind
import firrtl.ir._

case class PrintfOp(
                     info: Info,
                     string: StringLit,
                     args: Seq[ExpressionResult],
                     condition: ExpressionResult
                   ) extends Assigner {

  val symbol: Symbol = PrintfOp.PrintfOpSymbol

  def run: FuncUnit = {
    val conditionValue = condition match {
      case e: IntExpressionResult  => e.apply() > 0
      case e: LongExpressionResult => e.apply() > 0L
      case e: BigExpressionResult  => e.apply() > Big(0)
    }
    if(conditionValue) {
      val currentArgValues = args.map {
        case e: IntExpressionResult  => e.apply()
        case e: LongExpressionResult => e.apply()
        case e: BigExpressionResult  => e.apply()
      }
      val formatString = string.escape
      print(executeVerilogPrint(formatString, currentArgValues))
    }
    () => Unit
  }

  def executeVerilogPrint(formatString: String, allArgs: Seq[Any]): String = {
    val outBuffer = new StringBuilder
    var s = formatString
    var args = allArgs

    while(s.nonEmpty) {
      s.indexOf("%") match {
        case -1 =>
          outBuffer ++= s
          s = ""
        case offset =>
          outBuffer ++= s.take(offset)
          s = s.drop(offset + 1)
          s.headOption match {
            case Some('%') =>
              outBuffer ++= "%"
              s = s.tail
            case Some('b') =>
              outBuffer ++= BigInt(args.head.toString).toString(2)
              args = args.tail
              s = s.tail
            case Some('c') =>
              outBuffer += BigInt(args.head.toString).toChar
              args = args.tail
              s = s.tail
            case Some(specifier)   =>
              //noinspection ScalaUnnecessaryParentheses
              outBuffer ++= (s"%$specifier").format(BigInt(args.head.toString))
              args = args.tail
              s = s.tail
            case _ =>
              s = ""
          }
      }
    }
    StringContext.treatEscapes(outBuffer.toString())
  }
}

object PrintfOp {
  val PrintfOpSymbol = Symbol("printfop", IntSize, UnsignedInt, WireKind, 1, 1, UIntType(IntWidth(1)), NoInfo)
  PrintfOpSymbol.index = 0
  PrintfOpSymbol.cardinalNumber = Int.MaxValue - 1 // this goes after everything except for StopOp
}