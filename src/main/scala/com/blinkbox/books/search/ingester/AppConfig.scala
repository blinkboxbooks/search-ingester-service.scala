package com.blinkbox.books.search.ingester

import com.blinkbox.books.config._
import com.blinkbox.books.rabbitmq.RabbitMqConsumer
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.typesafe.config.Config
import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

case class AppConfig(
  index: String,
  retryTime: FiniteDuration,
  requestTimeout: FiniteDuration,
  bookMetadataInput: RabbitMqConsumer.QueueConfiguration,
  bookMetadataErrorOutput: PublisherConfiguration,
  priceDataInput: RabbitMqConsumer.QueueConfiguration,
  priceDataErrorOutput: PublisherConfiguration,
  solr: SolrConfig)

case class SolrConfig(url: URL)

object AppConfig {
  def apply(config: Config): AppConfig = {
    val serviceConfig = config.getConfig("service.searchIngester")
    AppConfig(
      serviceConfig.getString("index"),
      serviceConfig.getDuration("retryTime", TimeUnit.MILLISECONDS).millis,
      serviceConfig.getDuration("requestTimeout", TimeUnit.MILLISECONDS).millis,
      RabbitMqConsumer.QueueConfiguration(serviceConfig.getConfig("bookMetadataInput")),
      RabbitMqConfirmedPublisher.PublisherConfiguration(serviceConfig.getConfig("bookMetadataErrorOutput")),
      RabbitMqConsumer.QueueConfiguration(serviceConfig.getConfig("priceDataInput")),
      RabbitMqConfirmedPublisher.PublisherConfiguration(serviceConfig.getConfig("priceDataErrorOutput")),
      SolrConfig(config.getConfig("solr")))
  }
}

object SolrConfig {
  def apply(config: Config): SolrConfig = SolrConfig(config.getUrl("url"))
}
