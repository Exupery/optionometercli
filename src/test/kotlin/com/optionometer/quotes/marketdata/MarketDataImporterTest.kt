package com.optionometer.quotes.marketdata

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.optionometer.main.AppConfiguration
import com.optionometer.models.Side
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(classes = [AppConfiguration::class])
class MarketDataImporterTest(
  @Autowired private val mapper: ObjectMapper
) {

  private val ticker = "DIS"

  private val mockWebServer = MockWebServer()
  private val optionChainResponseJson =
    String(ClassLoader.getSystemResourceAsStream("optionchaintestresponse.json")!!.readAllBytes())

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
    assertThrows<MismatchedInputException> {
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
      val chains = importer.fetchOptionChains(ticker, 0, 30)
      assertTrue(chains.isEmpty())
    }
  }

  @Test
  fun `verify DTE params are set`() {
    enqueueOptionChainResponse()

    importer.fetchOptionChains(ticker, 0, 30)
    val request = mockWebServer.takeRequest()
    assertNotNull(request.path)
    val path = request.path!!
    assertTrue(path.contains("from="))
    assertTrue(path.contains("to="))
  }

  @Test
  fun `verify strike limit param is set`() {
    enqueueOptionChainResponse()

    importer.fetchOptionChains(ticker, 0, 30)
    val request = mockWebServer.takeRequest()
    assertNotNull(request.path)
    val path = request.path!!
    assertTrue(path.contains("strikeLimit=10"))
  }

  @Test
  fun `verify option chains are grouped by expiry`() {
    enqueueOptionChainResponse()

    val optionChains = importer.fetchOptionChains(ticker, 0, 30)
    // Test is meaningless if test response has fewer than two chains
    assertTrue(optionChains.size >= 2)
    val expirations = optionChains.map { it.expiry }
    assertEquals(optionChains.size, expirations.size)
  }

  @Test
  fun `verify options are associated with correct side`() {
    enqueueOptionChainResponse()

    val optionChains = importer.fetchOptionChains(ticker, 0, 30)
    assertFalse(optionChains.isEmpty())
    optionChains.forEach { optionChain ->
      assertTrue(optionChain.calls.all { it.side == Side.CALL })
      assertTrue(optionChain.puts.all { it.side == Side.PUT })
    }
  }

  @Test
  fun `verify options are put in correct option chain`() {
    enqueueOptionChainResponse()

    val optionChains = importer.fetchOptionChains(ticker, 0, 30)
    assertFalse(optionChains.isEmpty())
    optionChains.forEach { optionChain ->
      val options = optionChain.calls + optionChain.puts
      assertTrue(options.all { it.expiry == optionChain.expiry.epochSecond })
    }
  }

  private fun enqueueOptionChainResponse() {
    val response = MockResponse()
      .setBody(optionChainResponseJson)
      .setResponseCode(200)
    mockWebServer.enqueue(response)
  }

}
