package com.optionometer.screener.scorers

import com.optionometer.models.Option
import com.optionometer.screener.Trade
import org.apache.commons.statistics.distribution.NormalDistribution
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

const val MAX_HIGH_SCORES_TO_RETURN = 100
private const val NUM_STANDARD_DEVIATIONS = 2

abstract class Scorer<R, S> {

  private val logger = LoggerFactory.getLogger(javaClass)

  protected val minAnnualReturn: Int = System.getProperty("MIN_ANNUAL_RETURN")?.toInt() ?: 1
  protected val minProbability: Int = System.getProperty("MIN_PROBABILITY")?.toInt() ?: 25
  protected val minProfitAmount: Double = System.getProperty("MIN_PROFIT_AMOUNT")?.toDouble() ?: 0.5

  fun score(trades: List<Trade>, underlyingPrice: Double): List<S> {
    logger.info("Scoring ${"%,d".format(trades.size)} trades")
    val rawScored = trades.mapNotNull {
      score(it, underlyingPrice)
    }

    return normalize(rawScored)
  }

  private fun score(trade: Trade, underlyingPrice: Double): R? {
    val sd = calculateOverallImpliedSd(trade, underlyingPrice)
    val sdPrices = StandardDeviationPrices(underlyingPrice, sd)
    val plByPrice = calcProfitLossByPrice(trade, sdPrices)

    if (plByPrice.all { it.value < minProfitAmount }) {
      return null
    }

    val probability = successProbability(plByPrice, underlyingPrice, sd)
    if (probability < minProbability) {
      return null
    }

    val dte = (trade.sells + trade.buys).first().dte
    val annualReturn = annualReturnScore(dte, probability, plByPrice, sdPrices, trade)
    if (annualReturn < minAnnualReturn) {
      return null
    }

    return score(
      trade,
      underlyingPrice,
      sd,
      sdPrices,
      plByPrice,
      probability,
      annualReturn
    )
  }

  protected abstract fun score(
    trade: Trade,
    underlyingPrice: Double,
    sd: Double,
    sdPrices: StandardDeviationPrices,
    plByPrice: Map<Int, Double>,
    probability: Double,
    annualReturn: Double
  ): R?

  protected abstract fun calculateOverallImpliedSd(trade: Trade, underlyingPrice: Double): Double

  protected abstract fun normalize(rawScoredTrades: List<R>): List<S>

  protected fun calculateImpliedSd(option: Option, underlyingPrice: Double): Double {
    // 1SD = stock price * iv * sqrt(dte / 365)
    return underlyingPrice * option.impliedVolatility * sqrt(option.dte.toDouble() / 365.0)
  }

  protected fun successProbability(
    plByPrice: Map<Int, Double>,
    underlyingPrice: Double,
    standardDeviation: Double
  ): Double {
    val profitPrices = plByPrice.keys.filter { price ->
      plByPrice[price]!! > minProfitAmount
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

  protected fun maxProfitLoss(
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

    val (losses, profits) = plByPrice.values.partition { it < minProfitAmount }
    val numLossesAtMaxLoss = losses.filter { it.equivalent(max2SdLoss) }.size
    val numProfitsAtMaxProfit = profits.filter { it.equivalent(max1SdProfit) }.size
    // For score favor trades with limited downside
    val score = max(numLossesAtMaxLoss, 1).toDouble() / max(losses.size, 1)
    return MaxProfitLoss(ratio, numProfitsAtMaxProfit, max1SdProfit, numLossesAtMaxLoss, max2SdLoss, score)
  }

  protected fun annualReturnScore(
    dte: Int,
    probability: Double,
    plByPrice: Map<Int, Double>,
    sdPrices: StandardDeviationPrices,
    trade: Trade
  ): Double {
    val avg = { pls: Map<Int, Double> ->
      pls.map { (price, pl) ->
        val sdBand = sdPrices.sdBand(price.toDouble())
        val multiplier = when (sdBand) {
          0, 1 -> 1.0
          2 -> {
            if (pl < minProfitAmount) {
              1.0
            } else {
              // Discount trades that require a large move to become profitable
              0.5
            }
          }

          else -> 0.0
        }
        multiplier * pl
      }.average()
    }
    val profitPls = plByPrice.filter { it.value > minProfitAmount }
    val lossPls = plByPrice.filter { it.value <= minProfitAmount }
    val profitAvg = avg(profitPls)
    val lossAvg = avg(lossPls)
    val numTradesPerYear = max(365 / max(dte, 7), 1)
    val profit = numTradesPerYear * (probability / 100) * (profitAvg.takeIf { !it.isNaN() } ?: 0.0)
    val loss = numTradesPerYear * ((100 - probability) / 100) * (lossAvg.takeIf { !it.isNaN() } ?: 0.0)
    // Add because loss is negative
    return (((profit + loss) * 100) / trade.requiredMargin()) * 100
  }

  protected fun scoreByNumProfitablePoints(
    plByPrice: Map<Int, Double>,
    sdPrices: StandardDeviationPrices
  ): Double {
    val numProfitable = plByPrice.filter { it.value > minProfitAmount }
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

  protected fun calcProfitLossByPrice(
    trade: Trade,
    sdPrices: StandardDeviationPrices
  ): Map<Int, Double> {
    return (sdPrices.lowerSd.toInt()..sdPrices.upperSd.toInt()).associateWith {
      trade.profitLossAtPrice(it.toDouble())
    }
  }

}

data class Score(
  val pricePointScore: Double,
  val numProfitablePointsScore: Double,
  val probability: Double,
  val maxLossRatio: Double,
  val annualizedReturn: Double,
  val deltaScore: Double,
  val hundredTradesScore: Int
)

data class MaxProfitLoss(
  val maxProfitToMaxLossRatio: Double,
  val numMaxProfit: Int,
  val maxProfit: Double,
  val numMaxLoss: Int,
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

  val upperSd = underlyingPrice + (standardDeviation * NUM_STANDARD_DEVIATIONS)
  val lowerSd = max(underlyingPrice - (standardDeviation * NUM_STANDARD_DEVIATIONS), 1.0)

  fun sdBand(price: Double): Int {
    val diff = abs(underlyingPrice - price)
    return (diff / standardDeviation).toInt() + 1
  }

  override fun toString(): String {
    return "$standardDeviation [$lowerSd - $upperSd]"
  }
}

fun Double.equivalent(other: Double): Boolean {
  return abs(other - this) < 0.01
}
