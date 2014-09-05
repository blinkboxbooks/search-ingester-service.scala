search-ingester
===============

Scala version of search ingester service. This consumes XML messages from RabbitMQ, containing metadata for new books, notifications for undistributed books, and book price updates.

## Installation

The service is built as a Scala fat jar, and uses the standard configuration and logging facilities as used by blinkbox books platform services.

The service is configured to receive messages from the same RabbitMQ queues as the previous (Java/Mule) version, as described at http://jira.blinkbox.local/confluence/display/REL/Search+Ingester+Service+-+Release+Note. These queues are configured in the reference configuration, so would normally not need to be specified in application config.

Updates are passed on to a configured instance of Solr, the URL for which is configured in the application configuration.

