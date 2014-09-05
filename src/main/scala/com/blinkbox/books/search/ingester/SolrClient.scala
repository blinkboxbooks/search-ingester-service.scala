package com.blinkbox.books.search.ingester

import akka.util.Timeout
import akka.actor.ActorRef
import akka.pattern.ask
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.IOException
import java.net.URI
import java.net.URL
import org.json4s.jackson.JsonMethods._
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import scala.concurrent.{ Promise, Future, ExecutionContext }
import spray.http._
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.http.ContentTypes._
import spray.httpx.RequestBuilding._

/**
 * Very basic Solr client for posting updates directly as XML.
 */
class SolrClient(config: SolrConfig, index: String, httpActor: ActorRef)(
  implicit val timeout: Timeout, implicit val ec: ExecutionContext) extends XmlHandler with StrictLogging {

  private val updateUrl = Uri(config.url.toExternalForm + s"/$index/update")

  // Pass on XML as an Update operation to Solr.
  def handleXml(data: String): Future[Unit] = {
    val request = HttpRequest(method = POST, uri = updateUrl, entity = HttpEntity(ContentType(MediaTypes.`application/xml`), data))
    (httpActor ? request)
      .mapTo[HttpResponse]
      .map(convertSolrResponse(_))
  }

  private def convertSolrResponse(response: HttpResponse) = response match {
    case HttpResponse(StatusCodes.OK, entity, _, _) => ()
    case res @ HttpResponse(status, entity, _, _) if (502 to 599).contains(status.intValue) =>
      throw new IOException(s"HTTP response status indicating temporary failure: $status")
    case res @ HttpResponse(status, entity, _, _) =>
      throw new RuntimeException(s"Unexpected HTTP response status: $status")
    case res @ _ => throw new RuntimeException("Unexpected response from book service: " + res)
  }

}
