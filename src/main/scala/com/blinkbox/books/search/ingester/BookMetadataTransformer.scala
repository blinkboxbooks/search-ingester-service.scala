package com.blinkbox.books.search.ingester

import akka.actor.ActorRef
import com.blinkbox.books.messaging._
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io._
import java.util.concurrent.TimeoutException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import scala.io.Source
import scala.util.Try
import scala.xml.XML
import spray.can.Http.ConnectionException

class BookMetadataTransformer(xmlHandler: SolrApi, errorHandler: ErrorHandler, retryInterval: FiniteDuration)
  extends ReliableEventHandler(errorHandler, retryInterval) with StrictLogging {

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] = {
    for (
      outputMsg <- Future(transformMessage(event.body.content));
      _ <- xmlHandler.handleUpdate(outputMsg)
    ) yield ()
  }

  override def isTemporaryFailure(e: Throwable) =
    e.isInstanceOf[IOException] || e.isInstanceOf[TimeoutException] || e.isInstanceOf[ConnectionException] ||
      Option(e.getCause).exists(isTemporaryFailure)

  /** Carry out XSLT transform on incoming XML. */
  private def transformMessage(bytes: Array[Byte]): String = {
    val xml = XML.load(new ByteArrayInputStream(bytes))

    // Get a suitable Transformer.
    val transformer = xml.label match {
      case "book" => bookTransformer
      case "undistribute" => undistributeTransformer
      case "book-price" => priceTransformer
      case _ => throw new IllegalArgumentException(s"Unexpected XML with root element: ${xml.label}")
    }

    // Transform the input.
    val xmlSource = new StreamSource(new ByteArrayInputStream(bytes))
    val output = new StringWriter
    transformer.transform(xmlSource, new StreamResult(output))
    output.toString
  }

  // Transformers used for incoming XML messages.
  // These are not thread safe hence an instance created for each access.
  private def bookTransformer = transformer(xsltSource(bookTransform))
  private def undistributeTransformer = transformer(xsltSource(undistributeTransform))
  private def priceTransformer = transformer(xsltSource(priceTransform))

  // Only read XSLT files once.
  private val bookTransform = fromFile("/book-to-solr.xsl")
  private val undistributeTransform = fromFile("/undistribute-book.xsl")
  private val priceTransform = fromFile("/price-to-solr.xsl")

  private def fromFile(filename: String) = Source.fromInputStream(getClass.getResourceAsStream(filename)).mkString
  private def transformer(xsltSource: StreamSource) = TransformerFactory.newInstance().newTransformer(xsltSource)
  private def xsltSource(content: String) = new StreamSource(new StringReader(content))

}