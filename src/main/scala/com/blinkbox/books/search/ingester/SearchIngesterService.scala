package com.blinkbox.books.search.ingester

import akka.actor.{ ActorSystem, Props }
import akka.io.IO
import akka.util.Timeout
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.messaging.ActorErrorHandler
import com.blinkbox.books.rabbitmq._
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.typesafe.scalalogging.slf4j.StrictLogging
import spray.can.Http

object SearchIngesterService extends App with Configuration with StrictLogging with Loggers {

  logger.info(s"Starting Search Ingester service")

  // Get configuration
  val appConfig = AppConfig(config)
  val consumerConnection = RabbitMq.reliableConnection(appConfig.rabbitMq)
  val publisherConnection = RabbitMq.recoveredConnection(appConfig.rabbitMq)

  // Initialise the actor system.
  implicit val system = ActorSystem("search-ingester-service")
  implicit val ec = system.dispatcher
  implicit val requestTimeout = Timeout(appConfig.requestTimeout)

  logger.debug("Initialising actors")

  private def publisher(config: PublisherConfiguration, actorName: String) =
    system.actorOf(Props(new RabbitMqConfirmedPublisher(publisherConnection, config)), name = actorName)

  val bookMsgErrorHandler = new ActorErrorHandler(publisher(appConfig.bookMetadataErrorOutput, "book-error-publisher"))
  val priceMsgErrorHandler = new ActorErrorHandler(publisher(appConfig.priceDataErrorOutput, "price-error-publisher"))

  // Initialise the Solr client.
  val httpActor = IO(Http)
  val solrClient = new SolrClient(appConfig.solr, appConfig.index, httpActor)

  val bookMetadataHandler = system.actorOf(Props(
    new BookMetadataTransformer(solrClient, bookMsgErrorHandler, appConfig.retryTime)), name = "book-metadata-handler")

  val priceDataHandler = system.actorOf(Props(
    new BookMetadataTransformer(solrClient, bookMsgErrorHandler, appConfig.retryTime)), name = "price-data-handler")

  // Create and start the actors that consume messages from RabbitMQ.
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.bookMetadataInput,
    "book-metadata-consumer", bookMetadataHandler)),
    name = "book-metadata-listener") ! RabbitMqConsumer.Init
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.priceDataInput,
    "price-data-consumer", priceDataHandler)),
    name = "price-data-listener") ! RabbitMqConsumer.Init

  logger.info("Started")
}

