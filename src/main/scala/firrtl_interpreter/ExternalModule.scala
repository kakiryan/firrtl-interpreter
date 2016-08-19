// See LICENSE for license details.

package firrtl_interpreter

import firrtl.ir.{Type, Expression}

import scala.collection._

class ExternalModule {

}

case class BlackBoxOutput(name: String,
                          implementation: BlackBoxImplementation,
                          dependendInputs: Seq[String],
                          tpe: Type
                         ) extends Expression {
  def execute(inputValues: Seq[Concrete]): Concrete = {
    implementation.execute(inputValues)
  }
  def serialize: String = s"BlackBoxOutput($name,$tpe)"
}

abstract class BlackBoxImplementation {
  def name: String
  def fullName(componentName: String): String = s"$name.$componentName"
  def execute(inputValues: Seq[Concrete]): Concrete
  def step: Unit = {

  }
  def outputDependencies(outputName: String): Seq[String]
}

abstract class BlackBoxFactory {
  def boxes = new mutable.HashMap[String, BlackBoxImplementation]

  def add(blackBox: BlackBoxImplementation): BlackBoxImplementation = {
    boxes(blackBox.name) = blackBox
    blackBox
  }
  def createInstance(instanceName: String, blackBoxName: String): Option[BlackBoxImplementation]
  def appliesTo(blackBoxName: String): Boolean

  def step: Unit = {
    boxes.values.foreach { box => box.step }
  }
}