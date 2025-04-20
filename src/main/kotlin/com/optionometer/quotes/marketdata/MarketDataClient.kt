package com.optionometer.quotes.marketdata

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component

private const val ROOT_ENDPOINT = "https://api.marketdata.app"
private const val OPTION_CHAIN_PATH = "/v1/options/chain"

@Component
class MarketDataClient {

  private val client = OkHttpClient()
  private val apiToken = this.javaClass.getResourceAsStream("/.marketdataapitoken")
    ?.bufferedReader()
    ?.readLines()
    ?.first()

  fun getOptionChain(
    ticker: String
  ) {
    val request = Request.Builder()
      .url("$ROOT_ENDPOINT$OPTION_CHAIN_PATH/$ticker")
      .header("Authorization", "Bearer $apiToken")
      .build()

    val response = client.newCall(request).execute()
    println(response) // TODO DELME
  }

}