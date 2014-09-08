package com.blinkbox.books.search.ingester

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.IOException
import java.net.URI
import java.net.URL
import org.json4s.jackson.JsonMethods._
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import scala.concurrent.{ Promise, Future, ExecutionContext }
import spray.http._
import spray.http.StatusCodes._
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.http.ContentTypes._
import spray.httpx.RequestBuilding._

/**
 * Basic API for interactions with Solr.
 */
trait SolrApi {
  def handleUpdate(data: String): Future[Unit]
}

/**
 * Very basic Solr client for posting updates directly as XML.
 */
class SolrClient(config: SolrConfig, index: String, httpActor: ActorRef)(
  implicit val timeout: Timeout, implicit val ec: ExecutionContext) extends SolrApi with StrictLogging {

  private val updateUrl = Uri(config.url.toExternalForm + s"/$index/update")

  // Pass on XML as an Update operation to Solr.
  def handleUpdate(data: String): Future[Unit] = {
    val entity = HttpEntity(ContentType(MediaTypes.`application/xml`).withCharset(HttpCharsets.`UTF-8`), data)
    val request = HttpRequest(method = POST, uri = updateUrl, entity = entity)
    (httpActor ? request)
      .mapTo[HttpResponse]
      .map(convertSolrResponse(_))
  }

  private def convertSolrResponse(response: HttpResponse) = response match {
    case HttpResponse(OK, entity, _, _) => ()
    case res @ HttpResponse(status, entity, _, _) if (502 to 599).contains(status.intValue) =>
      throw new IOException(s"HTTP response status indicating temporary failure: $status")
    case res @ HttpResponse(status, entity, _, _) =>
      throw new RuntimeException(s"Unexpected HTTP response status: $status")
    case res @ _ => throw new RuntimeException("Unexpected response from book service: " + res)
  }

}
