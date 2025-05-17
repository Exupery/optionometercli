package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.screener.Normalizer.normalize
import org.apache.commons.statistics.distribution.NormalDistribution
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private const val MAX_HIGH_SCORES_TO_RETURN = 100
private const val MIN_PROFIT_AMOUNT = 0.15

object Scorer {

  fun score(trades: List<Trade>, underlyingPrice: Double): List<ScoredTrade> {
    val rawScored = trades.mapNotNull {
      score(it, underlyingPrice)
    }

    return normalize(rawScored).sortedByDescending {
      it.score
    }.filter {
      it.score > 0
    }.take(MAX_HIGH_SCORES_TO_RETURN)
  }

  private fun score(trade: Trade, underlyingPrice: Double): RawScoredTrade? {
    val sd = (trade.sells + trade.buys).map { calculateImpliedSd(it, underlyingPrice) }.average()
    val sdPrices = StandardDeviationPrices(underlyingPrice, sd)
    val plByPrice = (sdPrices.threeSdDown.toInt()..sdPrices.threeSdUp.toInt()).associateWith {
      trade.profitLossAtPrice(it.toDouble())
    }

    // If trade is unprofitable (or barely profitable) at
    // all price points return early
    if (plByPrice.all { it.value < MIN_PROFIT_AMOUNT }) {
      return null
    }

    val scoreByPricePoints = scoreByPricePoints(plByPrice, underlyingPrice, sd)
    val scoreByNumProfitablePoints = scoreByNumProfitablePoints(plByPrice, sdPrices)
    val probability = successProbability(plByPrice, underlyingPrice, sd)
    val maxProfitLoss = maxProfitLoss(plByPrice, sdPrices)
    val dte = (trade.sells + trade.buys).first().dte
    val annualReturn = annualReturnScore(dte, probability, plByPrice, sdPrices)
    val deltas = deltas(trade)
    val score = Score(
      scoreByPricePoints,
      scoreByNumProfitablePoints,
      probability,
      maxProfitLoss.score,
      annualReturn,
      deltas.deltaScore
    )

    return RawScoredTrade(score, plByPrice, sdPrices, maxProfitLoss, deltas.tradeDelta, trade)
  }

  private fun calculateImpliedSd(option: Option, underlyingPrice: Double): Double {
    // 1SD = stock price * iv * sqrt(dte / 365)
    return underlyingPrice * option.impliedVolatility * sqrt(option.dte.toDouble() / 365.0)
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

  private fun scoreByNumProfitablePoints(
    plByPrice: Map<Int, Double>,
    sdPrices: StandardDeviationPrices
  ): Double {
    val numProfitable = plByPrice.filter { it.value > MIN_PROFIT_AMOUNT }
    val weightedByBand = numProfitable.map { (price, _) ->
      val sdBand = sdPrices.sdBand(price.toDouble())
      when (sdBand) {
        0, 1 -> 4
        2 -> 2
        3 -> 1
        else -> 0
      }
    }.sum()
    return (weightedByBand.toDouble() / plByPrice.size) * 100
  }

  private fun successProbability(
    plByPrice: Map<Int, Double>,
    underlyingPrice: Double,
    standardDeviation: Double
  ): Double {
    val profitPrices = plByPrice.keys.filter { price ->
      plByPrice[price]!! > MIN_PROFIT_AMOUNT
    }
    val profitRanges = ranges(profitPrices)

    val normalDistribution = NormalDistribution.of(underlyingPrice, standardDeviation)
    return profitRanges.withIndex().map { indexedValue ->
      if (indexedValue.index % 2 != 0) {
        return@map 0.0
      }
      val rangeBegin = indexedValue.value
      val rangeEnd = profitRanges[indexedValue.index + 1]
      val probability = normalDistribution.probability(rangeBegin.toDouble(), rangeEnd.toDouble())
      probability * 100
    }.sum()
  }

  private fun ranges(
    prices: List<Int>
  ): List<Int> {
    if (prices.isEmpty()) {
      return emptyList()
    }
    val sorted = prices.sorted()
    val ranges = mutableListOf<Int>()
    var lastPrice = sorted.first()
    sorted.sorted().forEach { price ->
      if (ranges.isEmpty()) {
        ranges.add(price)
        lastPrice = price
        return@forEach
      }

      if (price != (lastPrice + 1)) {
        ranges.add(lastPrice)
        ranges.add(price)
      }

      lastPrice = price
    }
    ranges.add(lastPrice)

    return ranges.toList()
  }

  private fun maxProfitLoss(
    plByPrice: Map<Int, Double>,
    sdPrices: StandardDeviationPrices
  ): MaxProfitLoss {
    val max1SdProfit = plByPrice.filter { sdPrices.sdBand(it.key.toDouble()) < 2 }.values.max()
    val max2SdLoss = plByPrice.filter { sdPrices.sdBand(it.key.toDouble()) < 3 }.values.min()
    val ratio = if (max2SdLoss < 0) {
      max1SdProfit / abs(max2SdLoss)
    } else {
      // Highly unlikely any trade would have a max loss of zero (or
      // be profitable at all points) but handle that gracefully
      max1SdProfit
    }
    // For score favor trades with limited downside
    val losses = plByPrice.values.filter { it < MIN_PROFIT_AMOUNT }
    val numLossesAtMaxLoss = losses.filter { it == max2SdLoss }.size
    val score = numLossesAtMaxLoss.toDouble() / losses.size
    return MaxProfitLoss(ratio, max1SdProfit, max2SdLoss, score)
  }

  private fun annualReturnScore(
    dte: Int,
    probability: Double,
    plByPrice: Map<Int, Double>,
    sdPrices: StandardDeviationPrices
  ): Double {
    val avg = { pls: Map<Int, Double> ->
      pls.map { (price, pl) ->
        val sdBand = sdPrices.sdBand(price.toDouble())
        val multiplier = when (sdBand) {
          0, 1 -> 1.0
          2 -> {
            if (pl < MIN_PROFIT_AMOUNT) {
              0.3
            } else {
              0.03
            }
          }

          else -> 0.0
        }
        multiplier * pl
      }.average()
    }
    val profitPls = plByPrice.filter { it.value > MIN_PROFIT_AMOUNT }
    val lossPls = plByPrice.filter { it.value <= MIN_PROFIT_AMOUNT }
    val profitAvg = avg(profitPls)
    val lossAvg = avg(lossPls)
    val numTradesPerYear = max(365 / dte, 1)
    val profit = numTradesPerYear * (probability / 100) * profitAvg
    val loss = numTradesPerYear * ((100 - probability) / 100) * lossAvg
    // Add because loss is negative
    return profit + loss
  }

  private fun deltas(
    trade: Trade
  ): Deltas {
    val sum = (trade.sells + trade.buys).sumOf { it.delta }
    val score = trade.buys.sumOf { abs(it.delta) } - trade.sells.sumOf { abs(it.delta) }

    return Deltas(sum, score)
  }

}

data class Score(
  val pricePointScore: Double,
  val numProfitablePointsScore: Double,
  val scoreByProbability: Double,
  val maxLossRatio: Double,
  val annualizedReturn: Double,
  val deltaScore: Double
)

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
  val trade: Trade
)

data class MaxProfitLoss(
  val maxProfitToMaxLossRatio: Double,
  val maxProfit: Double,
  val maxLoss: Double,
  val score: Double
)

data class Deltas(
  val tradeDelta: Double,
  val deltaScore: Double
)

class StandardDeviationPrices(
  private val underlyingPrice: Double,
  private val standardDeviation: Double
) {

  val threeSdUp = underlyingPrice + (standardDeviation * 3)
  val threeSdDown = max(underlyingPrice - (standardDeviation * 3), 0.0)

  fun sdBand(price: Double): Int {
    val diff = abs(underlyingPrice - price)
    return (diff / standardDeviation).toInt() + 1
  }

  override fun toString(): String {
    return "$standardDeviation [$threeSdDown - $threeSdUp]"
  }
}
