package com.optionometer.screener

import com.optionometer.models.OptionChain
import com.optionometer.quotes.Importer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Screener(
  private val importer: Importer,
  private val minDays: Int,
  private val maxDays: Int,
  private val numLegs: String
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun screen(ticker: String) {
    if (minDays >= maxDays) {
      logger.error("Minimum days to expiration ($minDays) must be less than max days ($maxDays)")
      return
    }
    logger.info("Screening option trades for $ticker between $minDays and $maxDays days to expiration")
    val optionChains = importer.fetchOptionChains(ticker, minDays, maxDays)
    if (optionChains.isEmpty()) {
      logger.warn("No options matching criteria")
      return
    }
    val scoredTrades = scoreTrades(optionChains)
    println(scoredTrades.first().firstOrNull()) // TODO DELME
  }

  private fun scoreTrades(optionChains: List<OptionChain>): List<List<ScoredTrade>> {
    return optionChains.map {
      TradeBuilder(it)
    }.map { tb ->
      val tradeTypes = numLegs.split(",")

      // Multileg trades
      val scored = tradeTypes.associateWith { type ->
        when (type) {
          "2" -> Scorer.score(tb.spreads(), tb.underlyingPrice)
          "3" -> Scorer.score(tb.threeLegTrades(), tb.underlyingPrice)
          "4" -> Scorer.score(tb.fourLegTrades(), tb.underlyingPrice)
          else -> emptyList()
        }
      }

      // Enhanced (leg only added after scoring) trades
      val enhanced = tradeTypes.map { type ->
        if (type.contains("+")) {
          val key = type.filter { it != '+' }
          val scoredTrades = scored.getOrDefault(key, emptyList()).map { it.trade }
          val enhancedTrades = tb.enhancedTrades(scoredTrades)
          Scorer.score(enhancedTrades, tb.underlyingPrice)
        } else {
          emptyList()
        }
      }.filter { it.isNotEmpty() }.flatten()

      // Named trades
      val named = tradeTypes.map { type ->
        when (type) {
          "condors" -> Scorer.score(tb.condors(), tb.underlyingPrice)
          "bullputspreads" -> Scorer.score(tb.bullPutSpreads(), tb.underlyingPrice)
          else -> emptyList()
        }
      }.filter { it.isNotEmpty() }.flatten()

      (scored.values.flatten() + enhanced + named).sortedByDescending { it.score }
    }
  }

}
