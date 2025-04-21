package com.optionometer.quotes.marketdata

import com.fasterxml.jackson.databind.ObjectMapper
import com.optionometer.quotes.Importer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val OPTION_CHAIN_PATH = "/v1/options/chain"
private const val RATE_LIMIT_REMAINING = "X-Api-Ratelimit-Remaining"

@Component
class MarketDataImporter(
  private val rootEndpoint: String,
  private val mapper: ObjectMapper
) : Importer {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Value("\${marketdata.maxStrikes}")
  private val maxStrikes: Int = 10

  private val client = OkHttpClient()
  private val apiToken = this.javaClass.getResourceAsStream("/.marketdataapitoken")
    ?.bufferedReader()
    ?.readLines()
    ?.first()

  override fun fetchOptionChains(
    ticker: String,
    minDaysToExpiration: Int,
    maxDaysToExpiration: Int
  ) {
    val from = dteToTime(minDaysToExpiration)
    val to = dteToTime(maxDaysToExpiration)
    val url = "$rootEndpoint$OPTION_CHAIN_PATH/$ticker/?from=$from&to=$to&strikeLimit=$maxStrikes"
    logger.info("GET $url")
    val request = Request.Builder()
      .url(url)
      .header("Authorization", "Bearer $apiToken")
      .build()

    val response = client.newCall(request).execute()
    val rateLimitRemaining = response.headers[RATE_LIMIT_REMAINING]
    logger.info("Daily rate limit remaining: $rateLimitRemaining")
    val responseCode = response.code
    if (responseCode >= 400) {
      logger.error("$responseCode ${response.body?.string()}")
      return
    }
    val optionChainResponse = mapper.readValue(response.body?.string(), OptionChainResponse::class.java)
  }

  private fun dteToTime(dte: Int): Long {
    return Instant.now().plus(dte.toLong(), ChronoUnit.DAYS).epochSecond
  }

  private fun convert(optionChainResponse: OptionChainResponse) {

  }
}