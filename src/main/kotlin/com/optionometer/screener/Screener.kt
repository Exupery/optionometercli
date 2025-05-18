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
    logger.info("Screening option trades for $ticker between $minDays and $maxDays days to expiration")
    val optionChains = importer.fetchOptionChains(ticker, minDays, maxDays)
    val scoredTrades = scoreTrades(optionChains)
    println(scoredTrades.first().first()) // TODO DELME
  }

  private fun findPotentialTradesByChain(optionChain: OptionChain): TradeBuilder {
    val tradeBuilder = TradeBuilder(optionChain)
    logger.info("Found ${"%,d".format(tradeBuilder.spreads().size)} spreads")
    logger.info("Found ${"%,d".format(tradeBuilder.threeLegTrades().size)} three leg trades")
    logger.info("Found ${"%,d".format(tradeBuilder.fourLegTrades().size)} four leg trades")
    return tradeBuilder
  }

  private fun scoreTrades(optionChains: List<OptionChain>): List<List<ScoredTrade>> {
    return optionChains.map {
      findPotentialTradesByChain(it)
    }.map { tb ->
      val tradeTypes = numLegs.split(",")
      val scored = tradeTypes.associateWith { type ->
        when (type) {
          "2" -> Scorer.score(tb.spreads(), tb.underlyingPrice)
          "3" -> Scorer.score(tb.threeLegTrades(), tb.underlyingPrice)
          "4" -> Scorer.score(tb.fourLegTrades(), tb.underlyingPrice)
          else -> emptyList()
        }
      }

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

      (scored.values.flatten() + enhanced).sortedByDescending { it.score }
    }
  }

}
