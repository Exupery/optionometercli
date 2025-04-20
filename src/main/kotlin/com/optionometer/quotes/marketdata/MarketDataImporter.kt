package com.optionometer.quotes.marketdata

import com.optionometer.quotes.Importer
import org.springframework.stereotype.Component

@Component
class MarketDataImporter(
  private val client: MarketDataClient
) : Importer {

  override fun fetchOptionChains(
    ticker: String,
    minDaysToExpiration: Int,
    maxDaysToExpiration: Int
  ) {
    client.getOptionChain(ticker)
  }
}