package com.blinkbox.books.search.ingester

import akka.actor.ActorRef
import com.blinkbox.books.messaging.ErrorHandler
import com.blinkbox.books.messaging.Event
import com.blinkbox.books.messaging.ReliableEventHandler
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.IOException
import java.util.concurrent.TimeoutException
import spray.can.Http.ConnectionException
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import scala.util.Try
import scala.xml.XML
import java.io.ByteArrayInputStream
import scala.xml.NodeSeq
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import java.io.StringReader
import java.io.InputStreamReader
import javax.xml.transform.stream.StreamResult
import java.io.StringWriter


class BookMetadataTransformer(xmlHandler: SolrApi, errorHandler: ErrorHandler, retryInterval: FiniteDuration)
  extends ReliableEventHandler(errorHandler, retryInterval) with StrictLogging {

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] = {
    for (
        outputMsg <- Future(transformMessage(event.body.content));
        _ <- xmlHandler.handleUpdate(outputMsg))
      yield ()
  }

  override def isTemporaryFailure(e: Throwable) =
    e.isInstanceOf[IOException] || e.isInstanceOf[TimeoutException] ||
      Option(e.getCause).exists(isTemporaryFailure)

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
  def bookTransformer = transformer(xsltSource("/book-to-solr.xsl"))
  def undistributeTransformer = transformer(xsltSource("/undistribute-book.xsl"))
  def priceTransformer = transformer(xsltSource("/price-to-solr.xsl"))

  private def transformer(xsltSource: StreamSource) = TransformerFactory.newInstance().newTransformer(xsltSource)
  private def xsltSource(filename: String) = new StreamSource(new InputStreamReader(getClass.getResourceAsStream(filename)))

}
