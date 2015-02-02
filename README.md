search-ingester
===============

This is a message-driven service that consumes XML messages from RabbitMQ containing metadata for new books, notifications for undistributed books, and book price updates.

It relies on common blinkbox Books libraries for message formats and reliable message handling.

## Installation

The service is built as a Scala fat jar, and uses the standard configuration and logging facilities as used by blinkbox Books platform services.

### Queues and Exchanges

The service is configured to receive messages from queues bound to fanout exchanges, for legacy reasons. These queues are configured in the reference configuration, so would normally not need to be specified in application config.

### Solr integration

Updates are passed on to a configured instance of Solr, the URL for which is configured in the application config.

***Note:*** The service has a configurable prefetch-limit, which limits the number of messages that can be in-flight at any one time. This effectively acts as an upper limit to the number of concurrent requests the service can make on Solr. The default can be overridden with the settings `service.searchIngester.bookMetadataInput.prefetchCount` and`service.searchIngester.priceMetadataInput.prefetchCount`.
