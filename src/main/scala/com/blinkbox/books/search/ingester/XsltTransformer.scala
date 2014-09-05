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

trait XmlHandler {
  def handleXml(data: String): Future[Unit]
}

class XsltTransformer(xmlHandler: XmlHandler, errorHandler: ErrorHandler, retryInterval: FiniteDuration)
  extends ReliableEventHandler(errorHandler, retryInterval) with StrictLogging {

  override def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] = {
    val xml = XML.load(new ByteArrayInputStream(event.body.content))

    // Get a suitable Transformer.
    val transformer = xml.label match {
      case "book" => bookTransformer
      case "undistribute" => undistributeTransformer
      case "book-price" => priceTransformer
      case _ => throw new IllegalArgumentException(s"Unexpected XML with root element: ${xml.label}")
    }

    // Transform the input.
    val xmlSource = new StreamSource(new ByteArrayInputStream(event.body.content))
    val output = new StringWriter
    transformer.transform(xmlSource, new StreamResult(output))

    // Pass it on.
    xmlHandler.handleXml(output.toString)
  }

  override def isTemporaryFailure(e: Throwable) =
    // TODO: Consider which exceptions may be returned from the Solr client. 
    e.isInstanceOf[IOException] || e.isInstanceOf[TimeoutException] ||
      Option(e.getCause).exists(isTemporaryFailure)

  // Transformers used for incoming XML messages.
  // These are not thread safe hence an instance created for each Actor.
  val bookTransformer = transformer(xsltSource("/book-to-solr.xsl"))
  val undistributeTransformer = transformer(xsltSource("/undistribute-book.xsl"))
  val priceTransformer = transformer(xsltSource("/price-to-solr.xsl"))

  private def transformer(xsltSource: StreamSource) = TransformerFactory.newInstance().newTransformer(xsltSource)
  private def xsltSource(filename: String) = new StreamSource(new InputStreamReader(getClass.getResourceAsStream(filename)))

}
