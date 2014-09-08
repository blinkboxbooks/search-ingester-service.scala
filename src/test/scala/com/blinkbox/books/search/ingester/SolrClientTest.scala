package com.blinkbox.books.search.ingester

import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe, ImplicitSender }
import akka.util.Timeout
import com.blinkbox.books.test.FailHelper
import com.blinkbox.books.test.MockitoSyrup
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import java.io.IOException
import java.io.StringReader
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FlatSpecLike
import org.scalatest.StreamlinedXmlEquality
import org.scalatest.concurrent.{ AsyncAssertions, Futures, ScalaFutures }
import org.scalatest.junit.JUnitRunner
import scala.concurrent.duration._
import scala.concurrent.Future
import spray.http._
import spray.http.StatusCodes._

@RunWith(classOf[JUnitRunner])
class SolrClientTest extends TestKit(ActorSystem("test-system")) with ImplicitSender with ScalaFutures
  with FlatSpecLike with BeforeAndAfter with MockitoSyrup with StreamlinedXmlEquality with FailHelper {

  implicit val ec = system.dispatcher

  class SolrTestFixture {
    def solrConfig(content: String) = SolrConfig(ConfigFactory.parseReader(new StringReader(content)))

    implicit val timeout = Timeout(1.second)
    val configStr = """url: "http://hostname:8983/solr""""
    val indexName = "testIndex"
    val httpActor = TestProbe()
    val client = new SolrClient(solrConfig(configStr), indexName, httpActor.ref)
    val update = <add><doc><field name="isbn">9781905010943</field></doc></add>.toString
  }

  "A configured Solr client" should "send message to Solr and handle OK status code" in new SolrTestFixture {
    // Call the client with an update.
    val result = client.handleUpdate(update)

    // Check the request.
    val request = httpActor.expectMsgClass(100.millis, classOf[HttpRequest])
    assert(request.uri == Uri(s"http://hostname:8983/solr/$indexName/update"))
    assert(request.entity.asString(HttpCharsets.`UTF-8`) == update, "Should pass through content as UTF-8")
    assert(request.acceptableContentType(List(ContentType(MediaTypes.`application/xml`))).isDefined)

    // Trigger and check the response.
    httpActor.send(httpActor.lastSender, HttpResponse(OK))
    assert(result.isReadyWithin(100.millis), "Should just complete without error")
  }

  it should "throw IOException for temporary failures" in new SolrTestFixture {
    for (status <- List(ServiceUnavailable, BadGateway, GatewayTimeout, InsufficientStorage, BandwidthLimitExceeded)) {
      val result = client.handleUpdate(update)
      val request = httpActor.expectMsgClass(100.millis, classOf[HttpRequest])

      httpActor.send(httpActor.lastSender, HttpResponse(status))

      failingWith[IOException](result)
    }
  }

  it should "throw Exception for unrecoverable failures" in new SolrTestFixture {
    for (status <- List(InternalServerError, BadRequest, Conflict, Forbidden, NotFound, Unauthorized)) {
      val result = client.handleUpdate(update)

      httpActor.expectMsgClass(100.millis, classOf[HttpRequest])
      httpActor.send(httpActor.lastSender, HttpResponse(status))

      failingWith[RuntimeException](result)
    }

  }

  "A Solr client configuration" should "complain if the URL isn't valid" in new SolrTestFixture {
    intercept[ConfigException] { solrConfig("""url: "not.a.valid.url" """) }
    intercept[ConfigException] { solrConfig("""url: "jdbc://not.a.valid.http.url" """) }
  }

}
