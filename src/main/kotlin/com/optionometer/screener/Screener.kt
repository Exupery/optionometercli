package com.optionometer.screener

import com.optionometer.models.OptionChain
import com.optionometer.quotes.Importer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Screener(
  private val importer: Importer,
  private val minDays: Int,
  private val maxDays: Int
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun screen(ticker: String) {
    logger.info("Screening option trades for $ticker between $minDays and $maxDays days to expiration")
    val optionChains = importer.fetchOptionChains(ticker, minDays, maxDays)
    val tradesByExpiry = optionChains.map { findPotentialTradesByChain(it) }
  }

  private fun findPotentialTradesByChain(optionChain: OptionChain) {

  }

}