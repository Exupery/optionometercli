package com.optionometer.quotes.marketdata

import com.fasterxml.jackson.databind.ObjectMapper
import com.optionometer.models.Option
import com.optionometer.models.OptionChain
import com.optionometer.models.Side
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
  ): List<OptionChain> {
    val from = dteToTime(minDaysToExpiration)
    val to = dteToTime(maxDaysToExpiration)
    val url = "$rootEndpoint$OPTION_CHAIN_PATH/$ticker/?from=$from&to=$to&strikeLimit=$maxStrikes"
    logger.info("Making GET call to $url for $ticker")
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
      return emptyList()
    }

    val optionChainResponse = mapper.readValue(response.body?.string(), OptionChainResponse::class.java)

    // TODO OPTIONALLY LIMIT TO FRIDAY EXPIRES ONLY
    val numFound = optionChainResponse.underlyingPrice.size
    val nearest = Instant.ofEpochSecond(optionChainResponse.expiration.min())
    val farthest = Instant.ofEpochSecond(optionChainResponse.expiration.max())
    val minStrike = optionChainResponse.strike.min()
    val maxStrike = optionChainResponse.strike.max()
    logger.info(
      "Found $numFound options between strikes $minStrike and $maxStrike " +
          "expiring between $nearest and $farthest"
    )

    return convertToChains(ticker, optionChainResponse)
  }

  private fun dteToTime(dte: Int): Long {
    return Instant.now().plus(dte.toLong(), ChronoUnit.DAYS).epochSecond
  }

  private fun convertToChains(
    ticker: String,
    optionChainResponse: OptionChainResponse
  ): List<OptionChain> {
    val options = optionChainResponse.strike.mapIndexed { idx, _ ->
      Option(
        optionChainResponse.optionSymbol[idx],
        optionChainResponse.strike[idx],
        Side.valueOf(optionChainResponse.side[idx].uppercase()),
        optionChainResponse.expiration[idx],
        optionChainResponse.dte[idx],
        optionChainResponse.bid[idx],
        optionChainResponse.ask[idx],
        optionChainResponse.iv[idx],
        optionChainResponse.delta[idx]
      )
    }.filterNot { it.impliedVolatility == 0.0 }

    val underlyingPrice = optionChainResponse.underlyingPrice.firstOrNull() ?: return emptyList()
    val byExpiry = options.groupBy { it.expiry }
    return byExpiry.map { (expiry, bar) ->
      val bySide = bar.groupBy { it.side }
      val calls = bySide[Side.CALL]?.sortedBy { it.strike } ?: emptyList()
      val puts = bySide[Side.PUT]?.sortedBy { it.strike } ?: emptyList()
      val expires = Instant.ofEpochSecond(expiry)
      OptionChain(ticker, underlyingPrice, expires, calls, puts)
    }.sortedBy { it.expiry }
  }
}
