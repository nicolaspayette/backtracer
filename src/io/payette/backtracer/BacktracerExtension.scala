package io.payette.backtracer

import java.awt.Frame
import java.awt.event.ActionEvent

import io.payette.backtracer.BibTexFormatter.formatBibTex
import javax.imageio.ImageIO
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.ImageIcon
import javax.swing.JButton
import org.nlogo.api
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultClassManager
import org.nlogo.api.PrimitiveManager
import org.nlogo.api.Reporter
import org.nlogo.app.App
import org.nlogo.app.AppFrame
import org.nlogo.core.LogoList
import org.nlogo.core.Syntax
import org.nlogo.core.Syntax.StringType
import org.nlogo.core.Syntax.ListType
import org.nlogo.core.Syntax.reporterSyntax
import org.nlogo.swing.ToolBar
import org.nlogo.window.GUIWorkspace
import org.nlogo.workspace

import scala.concurrent.Await
import scala.concurrent.duration._

class BacktracerExtension extends DefaultClassManager {

  private var app: App = _

  def load(primManager: PrimitiveManager): Unit =
    List(
      GetSnippetsPrim,
      GetUrlsPrim,
      GetDoisPrim,
      GetCitationsPrim,
      GetBibtexPrim
    ).foreach(prim =>
      primManager.addPrimitive(makePrimName(prim), prim)
    )

  def makePrimName(obj: Any): String =
    obj.getClass.getSimpleName
      .split(raw"(?=\p{Upper})")
      .map(_.toLowerCase)
      .filterNot(_ == "prim$")
      .mkString("-")

  override def runOnce(extensionManager: api.ExtensionManager): Unit = {
    app = getApp(extensionManager)
    val button = new BacktracerButton(app.frame, getCode _)
    val toolBar = app.tabs.codeTab.toolBar
    val i = toolBar.getComponents.indexWhere { component =>
      component.isInstanceOf[JButton] && component.asInstanceOf[JButton].getToolTipText == button.getToolTipText
    }
    if (i == -1) {
      toolBar.add(new ToolBar.Separator)
      toolBar.add(button)
    } else {
      toolBar.remove(i)
      toolBar.add(button, i)
    }
  }

  private def getApp(extensionManager: api.ExtensionManager): App =
    Seq(extensionManager)
      .collect { case em: workspace.ExtensionManager => em.workspace }
      .collect { case ws: GUIWorkspace => ws }
      .flatMap { ws => ws.getFrame.asInstanceOf[AppFrame].getLinkChildren }
      .collect { case app: App => app }
      .head

  def getCode: String = app.tabs.codeTab.getText

  object GetSnippetsPrim extends Reporter {
    override def report(args: Array[Argument], context: Context): AnyRef =
      LogoList.fromVector(Snippets.extractFromCode(getCode).map(_.text))
    override def getSyntax: Syntax = reporterSyntax(ret = ListType)
  }

  object GetUrlsPrim extends Reporter {
    override def report(args: Array[Argument], context: Context): AnyRef =
      LogoList.fromVector(Snippets.extractFromCode(getCode).flatMap(_.urls).map(_.toString))
    override def getSyntax: Syntax = reporterSyntax(ret = ListType)
  }

  object GetDoisPrim extends Reporter {
    override def report(args: Array[Argument], context: Context): AnyRef =
      LogoList.fromVector(Snippets.extractFromCode(getCode).flatMap(_.dois).map(_.toString))
    override def getSyntax: Syntax = reporterSyntax(ret = ListType)
  }

  object GetCitationsPrim extends Reporter {
    override def report(args: Array[Argument], context: Context): AnyRef =
      LogoList.fromVector(
        Snippets.extractFromCode(getCode)
          .flatMap(snippet => Await.result(snippet.citations, 5.seconds))
          .map(_.text)
      )
    override def getSyntax: Syntax = reporterSyntax(ret = ListType)
  }

  object GetBibtexPrim extends Reporter {
    override def report(args: Array[Argument], context: Context): AnyRef =
      Snippets.extractFromCode(getCode)
        .flatMap(snippet => Await.result(snippet.bibTexEntries, 5.seconds))
        .map(entry => formatBibTex(entry.text))
        .mkString("\n")
    override def getSyntax: Syntax = reporterSyntax(ret = StringType)
  }

}

private class BacktracerButton(
  ownerFrame: Frame,
  getCode: () => String
) extends JButton() {
  setAction(new AbstractAction() {
    putValue(Action.SMALL_ICON, new ImageIcon(ImageIO.read(getClass.getResource("/backtracer.png"))))
    //putValue(Action.NAME, "BT")
    putValue(Action.SHORT_DESCRIPTION, "backtracer")
    override def actionPerformed(e: ActionEvent): Unit = {
      val dialog = new BacktracerDialog(ownerFrame, getCode)
      dialog.setVisible(true)
    }
  })
}