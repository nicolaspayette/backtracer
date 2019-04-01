package io.payette.qual2rule

import org.nlogo.api._
import org.nlogo.core.Syntax
import org.nlogo.core.Syntax.StringType
import org.nlogo.core.Syntax.reporterSyntax

class Qual2RuleExtension extends DefaultClassManager {

  def load(primManager: PrimitiveManager): Unit = List(AboutPrim)
    .foreach(prim => primManager.addPrimitive(makePrimName(prim), prim))

  def makePrimName(obj: Any): String =
    obj.getClass.getSimpleName
      .split("(?=\\p{Upper})")
      .map(_.toLowerCase)
      .filterNot(_ == "prim$")
      .mkString("-")
}

object AboutPrim extends Reporter {

  override def report(args: Array[Argument], context: Context): AnyRef = "What should it do?"

  override def getSyntax: Syntax = reporterSyntax(ret = StringType)

}