package io.payette.backtracer

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Desktop.getDesktop
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.ActionEvent

import javax.swing.AbstractAction
import javax.swing.AbstractListModel
import javax.swing.Action
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JEditorPane
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JSplitPane.HORIZONTAL_SPLIT
import javax.swing.ListCellRenderer
import javax.swing.event.HyperlinkEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import org.nlogo.app.infotab.InfoFormatter
import org.nlogo.awt.Fonts
import org.nlogo.awt.Positioning
import org.nlogo.swing.Utils.addEscKeyAction

class BacktracerDialog(
  ownerFrame: Frame,
  getCode: () => String
) extends JDialog(ownerFrame, "Backtracer", true) {
  val snippets: Vector[Snippet] = Snippets.extractFromCode(getCode())
  Fonts.adjustDefaultFont(this)

  val snippetsList = new SnippetsList(snippets)
  val snippetsPane = new SnippetsPane(snippetsList)
  val splitPane = new JSplitPane(HORIZONTAL_SPLIT) {
    setTopComponent(new JScrollPane(snippetsList))
    setBottomComponent(new JScrollPane(snippetsPane))
  }

  val closeAction = new AbstractAction() {
    putValue(Action.NAME, "Close")
    override def actionPerformed(e: ActionEvent): Unit = setVisible(false)
  }
  val closeButton = new JButton(closeAction)
  addEscKeyAction(this, closeAction)
  getRootPane.setDefaultButton(closeButton)

  val refreshAction = new AbstractAction() {
    putValue(Action.NAME, "Refresh")
    override def actionPerformed(e: ActionEvent): Unit = snippetsPane.update()
  }

  add(new JPanel {
    add(closeButton)
    add(new JButton(refreshAction))
  }, BorderLayout.PAGE_END)

  add(splitPane, BorderLayout.CENTER)
  splitPane.setDividerLocation(0.4)
  pack()
  Positioning.center(this, ownerFrame)
//  if (snippetsList.getModel.getSize > 0) snippetsList.setSelectedIndex(0)

  override def getPreferredSize: Dimension = new Dimension(1000, 600)

}

class SnippetsPane(snippetsList: SnippetsList) extends JEditorPane() with ListSelectionListener {

  snippetsList.addListSelectionListener(this)

  setContentType("text/html")
  setEditable(false)
  addHyperlinkListener { e =>
    if (e.getEventType eq HyperlinkEvent.EventType.ACTIVATED) {
      getDesktop.browse(e.getURL.toURI)
    }
  }

  def update(): Unit = {
    val snippet: Snippet = snippetsList.getSelectedValue()
    setText(InfoFormatter(snippet.asMarkDown))
    setCaretPosition(0)
  }

  override def valueChanged(e: ListSelectionEvent): Unit = update()

}

class SnippetsList(snippets: IndexedSeq[Snippet]) extends JList[Snippet]() {
  setFont(Fonts.monospacedFont)
  setModel(
    new AbstractListModel[Snippet]() {
      override def getSize: Int = snippets.length
      override def getElementAt(i: Int) = snippets(i)
    }
  )
  setCellRenderer(new SnippetListCellRenderer(snippets))
}

class SnippetListCellRenderer(snippets: IndexedSeq[Snippet]) extends ListCellRenderer[Snippet] {
  val lineNoLength = (0 +: snippets.map(_.lineNo.toString.length)).max
  val maxTextLength = 40
  val defaultListCellRenderer = new DefaultListCellRenderer()
  override def getListCellRendererComponent(
    list: JList[_ <: Snippet], value: Snippet, index: Int, isSelected: Boolean, cellHasFocus: Boolean
  ): Component = defaultListCellRenderer.getListCellRendererComponent(
    list: JList[_], snippetToString(value), index, isSelected, cellHasFocus
  )
  def snippetToString(snippet: Snippet): AnyRef = {
    val paddedLineNo = snippet.lineNo.formatted(s"%${lineNoLength}d")
    val text = snippet.text.lines.mkString(" ")
    val truncatedText = if (text.length <= maxTextLength) text else text.take(maxTextLength - 3) + "..."
    paddedLineNo + " ⋅ " + truncatedText
  }
}