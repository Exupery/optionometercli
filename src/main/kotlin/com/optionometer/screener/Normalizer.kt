package com.optionometer.screener

import com.optionometer.screener.scorers.RawScoredTrade
import com.optionometer.screener.scorers.ScoredTrade
import org.slf4j.LoggerFactory
import java.util.UUID

object Normalizer {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val weigher = Weigher()

  fun normalize(rawScoredTrades: List<RawScoredTrade>): List<ScoredTrade> {
    logger.info("Normalizing ${"%,d".format(rawScoredTrades.size)} trades")
    val cohort = rawScoredTrades.associateBy { generateTradeUuid(it.trade) }
    val pricePointScores = normalize(cohort.map { (id, rst) -> id to rst.score.pricePointScore })
    val numProfitPointScores = normalize(cohort.map { (id, rst) -> id to rst.score.numProfitablePointsScore })
    val probabilityScores = normalize(cohort.map { (id, rst) -> id to rst.score.scoreByProbability })
    val maxProfitLossScores = normalize(cohort.map { (id, rst) -> id to rst.score.maxLossRatio })
    val annualReturnScores = normalize(cohort.map { (id, rst) -> id to rst.score.annualizedReturn })
    val deltaScores = normalize(cohort.map { (id, rst) -> id to rst.score.deltaScore })
    val hundredTradesScores = normalize(
      cohort.map { (id, rst) -> id to rst.score.hundredTradesScore.toDouble() },
      (System.getProperty("HUNDRED_TRADE_WEIGHT")?.toInt() ?: -1) <= 1
    )

    return cohort.map { (id, rst) ->
      val score = weigher.scoreByWeight(
        pricePointScores.getOrDefault(id, 0.0),
        numProfitPointScores.getOrDefault(id, 0.0),
        probabilityScores.getOrDefault(id, 0.0),
        maxProfitLossScores.getOrDefault(id, 0.0),
        annualReturnScores.getOrDefault(id, 0.0),
        deltaScores.getOrDefault(id, 0.0),
        hundredTradesScores.getOrDefault(id, 0.0)
      )
      scoredTradeFromRawScoredTrade(score.toInt(), rst)
    }
  }

  private fun normalize(
    rawScores: List<Pair<String, Double>>,
    ignoreNegativeScores: Boolean = true
  ): Map<String, Double> {
    val cohortSize = rawScores.size
    val interval = 100.0 / (cohortSize - 1)
    val sorted = rawScores.sortedBy { it.second }
    return sorted.withIndex().associate { indexedValue ->
      // Only use actual percentile for positive non-zero scores
      val percentile = if (ignoreNegativeScores && indexedValue.value.second <= 0.0) {
        0.0
      } else {
        indexedValue.index * interval
      }
      indexedValue.value.first to percentile
    }
  }

  private fun scoredTradeFromRawScoredTrade(score: Int, raw: RawScoredTrade): ScoredTrade {
    return ScoredTrade(
      score,
      raw.plByPrice,
      raw.sdPrices,
      raw.score.scoreByProbability,
      raw.maxProfitLoss,
      raw.score.annualizedReturn,
      raw.tradeDelta,
      raw.score.hundredTradesScore,
      raw.trade
    )
  }

  private fun generateTradeUuid(trade: Trade): String {
    val str = "B${trade.buys.map { it.symbol }}-S${trade.sells.map { it.symbol }}"
    val uuid = UUID.nameUUIDFromBytes(str.toByteArray())
    return uuid.toString()
  }

}
