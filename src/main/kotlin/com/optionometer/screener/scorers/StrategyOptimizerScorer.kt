package com.optionometer.screener.scorers

import com.optionometer.screener.Normalizer
import com.optionometer.screener.Trade
import kotlin.math.abs
import kotlin.math.max

class StrategyOptimizerScorer : Scorer<RawScoredTrade, ScoredTrade>() {

  override fun score(
    trade: Trade,
    underlyingPrice: Double,
    sd: Double,
    sdPrices: StandardDeviationPrices,
    plByPrice: Map<Int, Double>,
    probability: Double,
    annualReturn: Double
  ): RawScoredTrade? {
    val scoreByPricePoints = scoreByPricePoints(plByPrice, underlyingPrice, sd)
    val maxProfitLoss = maxProfitLoss(plByPrice, sdPrices)
    val deltas = deltas(trade)
    val hundredTradesScore = hundredTrades(plByPrice, probability)
    val score = Score(
      scoreByPricePoints,
      scoreByNumProfitablePoints(plByPrice, sdPrices),
      probability,
      maxProfitLoss.score,
      annualReturn,
      deltas.deltaScore,
      hundredTradesScore
    )

    return RawScoredTrade(score, plByPrice, sdPrices, maxProfitLoss, deltas.tradeDelta, trade)
  }

  override fun calculateOverallImpliedSd(trade: Trade, underlyingPrice: Double): Double {
    return (trade.sells + trade.buys).map { calculateImpliedSd(it, underlyingPrice) }.average()
  }

  override fun normalize(rawScoredTrades: List<RawScoredTrade>): List<ScoredTrade> {
    return Normalizer.normalize(rawScoredTrades).sortedByDescending {
      it.score
    }.filter {
      it.score > 0
    }.take(MAX_HIGH_SCORES_TO_RETURN)
  }

  private fun scoreByPricePoints(
    plByPrice: Map<Int, Double>,
    underlyingPrice: Double,
    standardDeviation: Double
  ): Double {
    return plByPrice.map { (price, pl) ->
      val diff = abs(underlyingPrice - price)
      val sdMultiple = if (diff < standardDeviation) {
        2.0 + (standardDeviation / max(diff, 1.0))
      } else {
        1.0 + (standardDeviation / diff)
      }

      pl * sdMultiple
    }.sum()
  }

  private fun deltas(
    trade: Trade
  ): Deltas {
    val sum = (trade.sells + trade.buys).sumOf { it.delta }
    val score = trade.buys.sumOf { abs(it.delta) } - trade.sells.sumOf { abs(it.delta) }

    return Deltas(sum, score)
  }

  private fun hundredTrades(
    plByPrice: Map<Int, Double>,
    probability: Double
  ): Int {
    val maxProfit = plByPrice.values.max()
    val numMaxProfit = plByPrice.values.filter { it.equivalent(maxProfit) }.size
    val numProfitable = plByPrice.values.filter { it > minProfitAmount }.size
    val targetProfit = if (numMaxProfit > (numProfitable / 2)) {
      maxProfit
    } else {
      plByPrice.values.filter { it > minProfitAmount }.average()
    }
    val maxLoss = plByPrice.values.min()
    val numMaxLoss = plByPrice.values.filter { it.equivalent(maxLoss) }.size
    val numLosses = plByPrice.values.filter { it < 0 }.size
    val typicalLoss = if (numMaxLoss > (numLosses / 2)) {
      maxLoss
    } else {
      plByPrice.values.filter { it < minProfitAmount }.average()
    }
    val numLosingTrades = 100 - probability.toInt()
    val losses = numLosingTrades * typicalLoss
    val wins = probability.toInt() * targetProfit

    return ((losses + wins) * 100).toInt()
  }

}

data class RawScoredTrade(
  val score: Score,
  val plByPrice: Map<Int, Double>,
  val sdPrices: StandardDeviationPrices,
  val maxProfitLoss: MaxProfitLoss,
  val tradeDelta: Double,
  val trade: Trade
)

data class ScoredTrade(
  val score: Int,
  val plByPrice: Map<Int, Double>,
  val sdPrices: StandardDeviationPrices,
  val successProbability: Double,
  val maxProfitLoss: MaxProfitLoss,
  val annualizedReturn: Double,
  val tradeDelta: Double,
  val hundredTrades: Int,
  val trade: Trade
)
