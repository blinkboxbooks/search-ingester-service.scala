search-ingester
===============

Scala version of search ingester service. This consumes XML messages from RabbitMQ, containing metadata for new books, notifications for undistributed books, and book price updates. (The current version does *not* handle sales rank data)

## Installation

The service is built as a Scala fat jar, and uses the standard configuration and logging facilities as used by blinkbox books platform services.

### Queues and Exchanges

The service is configured to receive messages from the same RabbitMQ queues as the previous (Java/Mule) version, as described at http://jira.blinkbox.local/confluence/display/REL/Search+Ingester+Service+-+Release+Note. These queues are configured in the reference configuration, so would normally not need to be specified in application config.

***Note:*** this version of the service will also declare an exchange called `Distribution.Book.Search.Exchange`, bound to the `Distribution.Book.Search` queue.

### Solr integration

Updates are passed on to a configured instance of Solr, the URL for which is configured in the application config.

***Note:*** The service has a configurable prefetch-limit, which limits the number of messages that can be in-flight at any one time. This effectively acts as an upper limit to the number of concurrent requests the service can make on Solr. The default can be overridden with the settings `service.searchIngester.bookMetadataInput.prefetchCount` and `service.searchIngester.priceMetadataInput.prefetchCount`.

