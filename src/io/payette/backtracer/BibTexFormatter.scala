package io.payette.backtracer

import java.io.StringReader
import java.io.StringWriter

import org.jbibtex.BibTeXFormatter
import org.jbibtex.BibTeXParser

object BibTexFormatter {
  def formatBibTex(bibTex: String): String = {
    val reader = new StringReader(bibTex)
    val writer = new StringWriter()
    new BibTeXFormatter().format(new BibTeXParser().parse(reader), writer)
    writer.toString
  }
}
