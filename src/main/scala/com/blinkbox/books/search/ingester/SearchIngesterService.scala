package com.blinkbox.books.search.ingester

import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.messaging.ActorErrorHandler
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.blinkbox.books.rabbitmq._
import com.typesafe.scalalogging.slf4j.StrictLogging

object SearchIngesterService extends App with Configuration with StrictLogging with Loggers {

  logger.info(s"Starting Credit Offer service")

  // Get configuration
  val appConfig = AppConfig(config)
  val rabbitMqConfig = RabbitMqConfig(config)
  val consumerConnection = RabbitMq.reliableConnection(RabbitMqConfig(config))
  val publisherConnection = RabbitMq.recoveredConnection(RabbitMqConfig(config))

  // Initialise the actor system.
  implicit val system = ActorSystem("credit-offer-service")
  implicit val ec = system.dispatcher
  implicit val requestTimeout = Timeout(appConfig.requestTimeout)

  logger.debug("Initialising actors")

  private def publisher(config: PublisherConfiguration, actorName: String) =
    system.actorOf(Props(new RabbitMqConfirmedPublisher(publisherConnection, config)), name = actorName)

  val bookMsgErrorHandler = new ActorErrorHandler(publisher(appConfig.bookMetadataErrorOutput, "book-error-publisher"))
  val priceMsgErrorHandler = new ActorErrorHandler(publisher(appConfig.priceDataErrorOutput, "price-error-publisher"))

  val solrClient = new SolrClient(appConfig.solr, appConfig.index)

  val bookMetadataHandler = system.actorOf(Props(
    new XsltTransformer(solrClient, bookMsgErrorHandler, appConfig.retryTime)), name = "book-metadata-handler")

  val priceDataHandler = system.actorOf(Props(
    new XsltTransformer(solrClient, bookMsgErrorHandler, appConfig.retryTime)), name = "price-data-handler")

  // Create and start the actors that consume messages from RabbitMQ.
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.bookMetadataInput,
    "book-metadata-consumer", bookMetadataHandler)),
    name = "book-metadata-listener") ! RabbitMqConsumer.Init
  system.actorOf(Props(new RabbitMqConsumer(consumerConnection.createChannel, appConfig.priceDataInput,
    "price-data-consumer", priceDataHandler)),
    name = "price-data-listener") ! RabbitMqConsumer.Init

  logger.info("Started")
}

