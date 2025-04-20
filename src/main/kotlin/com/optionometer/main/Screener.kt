package com.optionometer.main

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

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun screen(ticker: String) {
    logger.info("Screening option trades for $ticker between $minDays and $maxDays days to expiration")
    importer.fetchOptionChains(ticker, minDays, maxDays)  // TODO
  }

}