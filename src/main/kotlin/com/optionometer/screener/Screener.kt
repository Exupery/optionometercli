package com.optionometer.screener

import com.optionometer.models.OptionChain
import com.optionometer.output.CsvWriter
import com.optionometer.quotes.Importer
import com.optionometer.screener.scorers.ScoredTrade
import com.optionometer.screener.scorers.StrategyOptimizerScorer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Screener(
  private val importer: Importer,
  private val minDays: Int,
  private val maxDays: Int,
  private val numLegs: String,
  private val csvWriter: CsvWriter
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun screen(
    ticker: String,
    screenerMode: Mode
  ) {
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

    when (screenerMode) {
      Mode.STRATEGY_OPTIMIZER -> ""
      Mode.BULL_PUT_SPREAD_SCREENER -> ""
    }

    val scoredTrades = scoreTrades(optionChains)
    if (scoredTrades.isEmpty() || scoredTrades.all { it.isEmpty() }) {
      logger.warn("No trades found with a positive score for selected criteria")
      return
    }
    csvWriter.write(ticker, scoredTrades)
    logger.info("Highest score trade for each expiry:")
    scoredTrades.forEach {
      logger.info(it.firstOrNull().toString())
    }
  }

  private fun scoreTrades(optionChains: List<OptionChain>): List<List<ScoredTrade>> {
    return optionChains.map {
      TradeBuilder(it)
    }.map { tb ->
      val scorer = StrategyOptimizerScorer()
      val tradeTypes = numLegs.split(",")

      // Multileg trades
      val scored = tradeTypes.associateWith { type ->
        when (type) {
          "2" -> scorer.score(tb.spreads(), tb.underlyingPrice)
          "3" -> scorer.score(tb.threeLegTrades(), tb.underlyingPrice)
          "4" -> scorer.score(tb.fourLegTrades(), tb.underlyingPrice)
          else -> emptyList()
        }
      }

      // Enhanced (leg only added after scoring) trades
      val enhanced = tradeTypes.map { type ->
        if (type.contains("+")) {
          val key = type.filter { it != '+' }
          val scoredTrades = scored.getOrDefault(key, emptyList()).map { it.trade }
          val enhancedTrades = tb.enhancedTrades(scoredTrades)
          scorer.score(enhancedTrades, tb.underlyingPrice)
        } else {
          emptyList()
        }
      }.filter { it.isNotEmpty() }.flatten()

      // Named trades
      val named = tradeTypes.map { type ->
        when (type) {
          "condors" -> scorer.score(tb.condors(), tb.underlyingPrice)
          "bullputspreads" -> scorer.score(tb.bullPutSpreads(), tb.underlyingPrice)
          else -> emptyList()
        }
      }.filter { it.isNotEmpty() }.flatten()

      (scored.values.flatten() + enhanced + named).sortedByDescending { it.score }
    }
  }

}

enum class Mode {
  STRATEGY_OPTIMIZER,
  BULL_PUT_SPREAD_SCREENER
}
