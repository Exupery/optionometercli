package com.optionometer.quotes.marketdata

import com.optionometer.quotes.Importer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component

private const val ROOT_ENDPOINT = "https://api.marketdata.app"
private const val OPTION_CHAIN_PATH = "/v1/options/chain"

@Component
class MarketDataImporter() : Importer {

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
    val request = Request.Builder()
      .url("$ROOT_ENDPOINT$OPTION_CHAIN_PATH/$ticker")
      .header("Authorization", "Bearer $apiToken")
      .build()

    val response = client.newCall(request).execute()
    println(response) // TODO DELME
  }
}