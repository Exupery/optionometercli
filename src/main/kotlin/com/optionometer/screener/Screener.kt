package com.optionometer.screener

import com.optionometer.quotes.Importer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Screener(
  private val importer: Importer,
  @Value("\${screener.expiration.minDays}") private val minDays: Int,
  @Value("\${screener.expiration.maxDays}") private val maxDays: Int
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun screen(ticker: String) {
    logger.info("Screening option trades for $ticker between $minDays and $maxDays days to expiration")
    val optionChains = importer.fetchOptionChains(ticker, minDays, maxDays)
  }

}