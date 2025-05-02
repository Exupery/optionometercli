package com.optionometer.screener

import java.util.UUID

object Normalizer {

  fun normalize(rawScoredTrades: List<RawScoredTrade>): List<ScoredTrade> {
    val cohort = rawScoredTrades.associateBy { generateTradeUuid(it.trade) }
    val pricePointScores = normalize(cohort.map { (id, rst) -> id to rst.score.pricePointScore })
    return cohort.map { (id, rst) ->
      val score = pricePointScores.getOrDefault(id, 0.0)
      scoredTradeFromRawScoredTrade(score.toInt(), rst)
    }
  }

  private fun normalize(rawScores: List<Pair<String, Double>>): Map<String, Double> {
    val cohortSize = rawScores.size
    val interval = 100.0 / (cohortSize - 1)
    val sorted = rawScores.sortedBy { it.second }
    return sorted.withIndex().associate { indexedValue ->
      val percentile = indexedValue.index * interval
      indexedValue.value.first to percentile
    }
  }

  private fun scoredTradeFromRawScoredTrade(score: Int, raw: RawScoredTrade): ScoredTrade {
    return ScoredTrade(
      score,
      raw.plByPrice,
      raw.sdPrices,
      raw.trade
    )
  }

  private fun generateTradeUuid(trade: Trade): String {
    val str = "B${trade.buys.map { it.symbol }}-S${trade.sells.map { it.symbol }}"
    val uuid = UUID.nameUUIDFromBytes(str.toByteArray())
    return uuid.toString()
  }

}
