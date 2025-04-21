package com.optionometer.quotes.marketdata

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.optionometer.main.AppConfiguration
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertNotNull

@SpringBootTest(classes = [AppConfiguration::class])
class MarketDataImporterTest(
  @Autowired private val mapper: ObjectMapper
) {

  private val ticker = "DIS"

  private val mockWebServer = MockWebServer()
  private val optionChainResponseJson =
    String(ClassLoader.getSystemResourceAsStream("optionchaintestresponse.json").readAllBytes())

  private val importer = MarketDataImporter(mockWebServer.url("").toString(), mapper)

  @Test
  fun `verify invalid json throws exception`() {
    val response = MockResponse()
      .setBody("DANGER")
      .setResponseCode(200)
    mockWebServer.enqueue(response)
    assertThrows<JsonParseException> {
      importer.fetchOptionChains(ticker, 0, 30)
    }
  }

  @Test
  fun `verify missing fields throws exception`() {
    val response = MockResponse()
      .setBody("""{"s":"ok"}""")
      .setResponseCode(200)
    mockWebServer.enqueue(response)
    assertThrows<MissingKotlinParameterException> {
      importer.fetchOptionChains(ticker, 0, 30)
    }
  }

  @Test
  fun `verify error is handled gracefully`() {
    val response = MockResponse()
      .setBody("""{"s":"bad request"}""")
      .setResponseCode(400)
    mockWebServer.enqueue(response)
    assertDoesNotThrow {
      importer.fetchOptionChains(ticker, 0, 30)
    }
  }

  @Test
  fun `verify DTE params are set`() {
    val response = MockResponse()
      .setBody(optionChainResponseJson)
      .setResponseCode(200)
    mockWebServer.enqueue(response)

    importer.fetchOptionChains(ticker, 0, 30)
    val request = mockWebServer.takeRequest()
    assertNotNull(request.path)
    val path = request.path!!
    assertTrue(path.contains("from="))
    assertTrue(path.contains("to="))
  }

  @Test
  fun `verify strike limit param is set`() {
    val response = MockResponse()
      .setBody(optionChainResponseJson)
      .setResponseCode(200)
    mockWebServer.enqueue(response)

    importer.fetchOptionChains(ticker, 0, 30)
    val request = mockWebServer.takeRequest()
    assertNotNull(request.path)
    val path = request.path!!
    assertTrue(path.contains("strikeLimit=10"))
  }

}