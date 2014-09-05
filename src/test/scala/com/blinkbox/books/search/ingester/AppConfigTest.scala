package com.blinkbox.books.search.ingester

import com.blinkbox.books.config.Configuration
import com.blinkbox.books.test.MockitoSyrup
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AppConfigTest extends FlatSpec with MockitoSyrup {

  // By loading Configuration in unit tests, we detect any discrepancies between
  // the reference config file and the AppConfig class early.
  "Application configuration" should "load without errors" in new Configuration {
    val appConfig = AppConfig(config)
    assert(appConfig.solr.url.toExternalForm == "http://localhost:8983/solr")
  }

}
