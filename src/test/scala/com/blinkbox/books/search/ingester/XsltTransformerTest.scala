package com.blinkbox.books.search.ingester

import akka.actor.{ ActorRef, ActorSystem, Props, Status }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.blinkbox.books.messaging._
import com.blinkbox.books.test.MockitoSyrup
import java.io.IOException
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.FlatSpecLike
import org.scalatest.StreamlinedXmlEquality
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXException
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Success
import scala.util.Failure
import org.mockito.ArgumentCaptor
import scala.xml.XML
import org.mockito.verification.VerificationMode

@RunWith(classOf[JUnitRunner])
class XsltTransformerTest extends TestKit(ActorSystem("test-system")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfter with MockitoSyrup with StreamlinedXmlEquality {

  "A book data processor" should "pass on transformed book distribute message" in new TestFixture {

    handler ! event("/example.book.xml")

    expectMsgType[Status.Success]

    checkSuccessfulResult("/example.book.out.xml")
    checkNoFailures()
  }

  it should "pass on transformed book undistribute message" in new TestFixture {

    handler ! event("/example.undistribute.xml")

    expectMsgType[Status.Success]

    checkSuccessfulResult("/example.undistribute.out.xml")
    checkNoFailures()
  }

  it should "pass on transformed price data message" in new TestFixture {
    handler ! event("/example.price.xml")

    expectMsgType[Status.Success]

    checkSuccessfulResult("/example.price.out.xml")
    checkNoFailures()
  }

  it should "pass on message to error handler for invalid incoming XML" in new TestFixture {

    val invalidEvent = Event.xml("This is not valid XML", EventHeader.apply("test"))
    handler ! invalidEvent

    // Should successfully complete processing of this message (i.e. not retry).
    expectMsgType[Status.Success]
    checkFailure[SAXException](invalidEvent)
  }

  it should "retry in case of a temporary error from its output" in new TestFixture {
    // Force a temporary exception to happen when passing on output command to Solr.
    val tempException = new IOException("Test exception")
    when(output.handleXml(anyString))
      .thenReturn(Future.failed(tempException))
      .thenReturn(Future.failed(tempException))
      .thenReturn(Future.successful(()))

    handler ! event("/example.book.xml")

    expectMsgType[Status.Success]

    checkSuccessfulResult("/example.book.out.xml", attempts = times(3))
    checkNoFailures()
  }

  it should "pass in message to error handler for non-temporary errors from its output" in new TestFixture {
    val unrecoverableError = new IllegalArgumentException("Test unrecoverable error")
    when(output.handleXml(anyString))
      .thenReturn(Future.failed(unrecoverableError))

    val inputEvent = event("/example.book.xml")
    handler ! inputEvent

    // Should successfully complete processing of this message (i.e. not retry).
    expectMsgType[Status.Success]
    verify(errorHandler).handleError(inputEvent, unrecoverableError)
  }

  trait TestFixture {

    val retryInterval = 100.millis

    // Define mocks and initialise them with default behaviour.
    val output = mock[XmlHandler]
    val errorHandler = mock[ErrorHandler]

    doReturn(Future.successful(())).when(errorHandler).handleError(any[Event], any[Throwable])
    doReturn(Future.successful(())).when(output).handleXml(anyString)

    // The actor under test.
    val handler = TestActorRef(Props(new XsltTransformer(output, errorHandler, retryInterval)))

    /** Create input event. */
    def event(inputFilename: String) = Event.xml(fileToString(inputFilename), EventHeader.apply("test"))

    /** Check that the event was processed successfully by checking the various outputs. */
    def checkSuccessfulResult(expectedFilename: String, attempts: VerificationMode = times(1)) = {
      val outputCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(output, attempts).handleXml(outputCaptor.capture)

      val expectedXml = XML.loadString(fileToString(expectedFilename))
      val producedXml = XML.loadString(outputCaptor.getValue)
      assert(expectedXml === producedXml)
    }

    def fileToString(inputFilename: String): String = {
      val input = getClass.getResourceAsStream(inputFilename)
      assert(input != null, s"Couldn't find test input $inputFilename")
      Source.fromInputStream(input).mkString
    }

    def checkNoFailures() = verify(errorHandler, times(0)).handleError(any[Event], any[Throwable])

    /** Check that event processing failed and was treated correctly. */
    def checkFailure[T <: Throwable](event: Event)(implicit manifest: Manifest[T]) {
      // Check no output was given.
      verify(output, times(0)).handleXml(anyString)

      // Check event was passed on to error handler, along with the expected exception.
      val expectedExceptionClass = manifest.runtimeClass.asInstanceOf[Class[T]]
      verify(errorHandler).handleError(eql(event), isA(expectedExceptionClass))
    }

  }

}
