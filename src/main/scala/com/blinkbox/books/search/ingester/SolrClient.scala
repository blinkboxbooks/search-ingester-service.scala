package com.blinkbox.books.search.ingester

import com.typesafe.scalalogging.slf4j.StrictLogging

class SolrClient(config: SolrConfig, index: String) extends XmlHandler with StrictLogging {

  // Pass on XML as an Update operation to Solr.
  def handleXml(data: String) = ???
}
