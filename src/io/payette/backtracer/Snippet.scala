package io.payette.backtracer

import com.linkedin.urls.Url
import com.linkedin.urls.detection.UrlDetector
import com.linkedin.urls.detection.UrlDetectorOptions
import com.softwaremill.sttp.Response
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.payette.backtracer.BibTexFormatter.formatBibTex

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

object Snippets {
  def extractFromCode(code: String): Vector[Snippet] =
    """(?s)<<(.*?)>>""".r
      .findAllIn(code)
      .matchData
      .map { m =>
        Snippet(
          lineNo = code.take(m.start).lines.length,
          text = m.subgroups.head.lines
            .map(_.dropWhile(_ == ';').trim)
            .filterNot(_.isEmpty)
            .mkString("\n")
        )
      }
      .toVector
}

case class Snippet(lineNo: Int, text: String) {
  implicit val sttpBackend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()
  implicit val executionContext = ExecutionContext.Implicits.global
  val urls: List[Url] = new UrlDetector(text, UrlDetectorOptions.Default).detect.asScala.toList
  val dois: List[DOI] =
    """(?i)10.\d{4,9}\/[-._;()/:A-Z0-9]+""".r
      .findAllIn(text).matchData.map(m => DOI(m.toString)).toList
  val citations: Future[List[Entry]] = queryDois("text/x-bibliography")
  val bibTexEntries: Future[List[Entry]] = queryDois("text/x-bibliography; style=bibtex")

  def asMarkDown: String = text + citationsAsMarkdown + bibTexAsMarkdown
  def citationsAsMarkdown: String =
    markdownSection("References", citations, entry => s"* ${entry.text}\n  ${entry.doi.uri}")
  def markdownSection(
    title: String,
    futureEntries: Future[List[Entry]],
    formatEntry: Entry => String
  ) =
    if (dois.isEmpty)
      ""
    else
      s"\n\n# $title\n\n" + (futureEntries.value match {
        case None => "(...still loading...)"
        case Some(Failure(e)) => "Error: " + e.getMessage
        case Some(Success(entries)) => entries.map(formatEntry).mkString("\n")
      })
  def bibTexAsMarkdown: String =
    markdownSection("BibTeX entries", bibTexEntries, entry => s"```\n${formatBibTex(entry.text)}\n```")
  def queryDois(accept: String): Future[List[Entry]] = {
    val eventualResponses: List[Future[(DOI, Response[String])]] =
      dois.map(doi =>
        sttp.get(doi.uri).header("Accept", accept).send().map(doi -> _)
      )
    Future.sequence(eventualResponses).map { pairs =>
      for {
        (doi, resp) <- pairs
        str <- resp.body.right.toSeq
      } yield Entry(doi, str.trim)
    }
  }

  case class DOI(text: String) {
    def uri = uri"https://doi.org/$text"
  }

  case class Entry(doi: DOI, text: String)

}
