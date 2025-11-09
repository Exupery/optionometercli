package com.optionometer.screener.scorers

import com.optionometer.models.Side
import com.optionometer.screener.Normalizer
import com.optionometer.screener.Trade
import kotlin.math.abs
import kotlin.math.max

class BullPutScorer : Scorer<RawScoredBullPut, ScoredBullPut>() {

  private val maxMargin: Int = System.getProperty("MAX_MARGIN")?.toInt() ?: 10_000

  override fun score(
    trade: Trade,
    underlyingPrice: Double,
    sd: Double,
    sdPrices: StandardDeviationPrices,
    plByPrice: Map<Int, Double>,
    probability: Double,
    annualReturn: Double
  ): RawScoredBullPut? {
    if (isNotValidBullPutSpread(trade)) {
      return null
    }

    val perSpreadPl = calcMaxProfitLoss(trade, probability, plByPrice)
    val numContracts = (maxMargin / (abs(perSpreadPl.maxLoss) * 100)).toInt()
    val tradePl = MaxProfitLoss(
      perSpreadPl.maxProfitToMaxLossRatio,
      perSpreadPl.numMaxProfit,
      perSpreadPl.maxProfit * numContracts,
      perSpreadPl.numMaxLoss,
      perSpreadPl.maxLoss * numContracts,
      perSpreadPl.score
    )
    val score = Score(
      underlyingPrice - trade.sells.first().strike,
      plByPrice.filter { it.value > 0 }.size.toDouble(),
      probability,
      tradePl.maxProfitToMaxLossRatio,
      annualReturnScore(trade, tradePl, probability),
      0.0,
      probability.toInt()
    )

    return RawScoredBullPut(
      score,
      plByPrice,
      sdPrices,
      tradePl,
      numContracts,
      trade
    )
  }

  override fun calculateOverallImpliedSd(trade: Trade, underlyingPrice: Double): Double {
    return calculateImpliedSd(trade.sells.first(), underlyingPrice)
  }

  override fun normalize(rawScoredTrades: List<RawScoredBullPut>): List<ScoredBullPut> {
    return Normalizer.normalizeBullPuts(rawScoredTrades).sortedByDescending {
      it.score
    }.filter {
      it.score > 0
    }.take(MAX_HIGH_SCORES_TO_RETURN)
  }

  private fun isNotValidBullPutSpread(trade: Trade): Boolean {
    if (trade.sells.size != 1 || trade.buys.size != 1) {
      return true
    }

    val sell = trade.sells.first()
    val buy = trade.buys.first()

    if (buy.side != Side.PUT || sell.side != Side.PUT) {
      return true
    }

    if (buy.strike >= sell.strike) {
      return true
    }

    return false
  }

  private fun calcMaxProfitLoss(
    trade: Trade,
    probability: Double,
    plByPrice: Map<Int, Double>
  ): MaxProfitLoss {
    val shortPut = trade.sells.first()
    val longPut = trade.buys.first()
    val credit = shortPut.bid - longPut.ask
    val maxLoss = (shortPut.strike - longPut.strike) - credit
    val ratio = credit / abs(maxLoss)
    // 13 - 0.16

    val profitLossCounts = plByPrice.values.partition { it > 0 }
    val numProfit = profitLossCounts.first.size
    val numLoss = profitLossCounts.second.size

    val score = ((ratio * 100) + probability) * (numProfit / plByPrice.size)

    return MaxProfitLoss(
      ratio,
      numProfit,
      credit,
      numLoss,
      maxLoss * -1,
      score
    )
  }

  private fun annualReturnScore(
    trade: Trade,
    tradePl: MaxProfitLoss,
    probability: Double
  ): Double {
    val shortPut = trade.sells.first()
    val spread = shortPut.strike - trade.buys.first().strike
    val spreadPercentOfSoldStrike = spread / shortPut.strike
    // Assume we'll close the trade early if it falls too far below short strike.
    val multiplier = if (spreadPercentOfSoldStrike < 0.01) {
      0.3
    } else {
      0.15
    }
    val typicalLoss = tradePl.maxLoss * multiplier
    val numTradesPerYear = max(365 / max(shortPut.dte, 7), 1)
    val profit = numTradesPerYear * (probability / 100) * tradePl.maxProfit
    val loss = numTradesPerYear * ((100 - probability) / 100) * typicalLoss

    // Loss is negative
    return (((profit + loss) * 100) / maxMargin) * 100
  }

}

data class RawScoredBullPut(
  val score: Score,
  val plByPrice: Map<Int, Double>,
  val sdPrices: StandardDeviationPrices,
  val maxProfitLoss: MaxProfitLoss,
  val numContracts: Int,
  val trade: Trade
)

data class ScoredBullPut(
  val score: Int,
  val successProbability: Double,
  val maxProfitLoss: MaxProfitLoss,
  val annualizedReturn: Double,
  val numContracts: Int,
  val trade: Trade
)
