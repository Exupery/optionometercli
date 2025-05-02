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
      val scoredSpreads = Scorer.score(tb.spreads(), tb.underlyingPrice)
      val scoredThreeLegs = Scorer.score(tb.threeLegTrades(), tb.underlyingPrice)
      val scoredFourLegs = Scorer.score(tb.fourLegTrades(), tb.underlyingPrice)

      (scoredSpreads + scoredThreeLegs + scoredFourLegs).sortedByDescending { it.score }
    }
  }

}
