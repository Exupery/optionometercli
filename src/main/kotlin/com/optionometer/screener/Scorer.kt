package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.screener.Normalizer.normalize
import kotlin.math.abs
import kotlin.math.sqrt

private const val MAX_HIGH_SCORES_TO_RETURN = 100

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
    if (plByPrice.all { it.value < 0.5 }) {
      return null
    }

    val scoreByPricePoints = scoreByPricePoints(sd, underlyingPrice, plByPrice)
    val score = Score(
      scoreByPricePoints
    )

    return RawScoredTrade(score, plByPrice, sdPrices, trade)
  }

  private fun calculateImpliedSd(option: Option, underlyingPrice: Double): Double {
    // 1SD = (iv / (sqrt(365 / dte))) * underlyingPrice
    return (option.impliedVolatility / sqrt(365.0 / option.dte.toDouble())) * underlyingPrice
  }

  private fun scoreByPricePoints(
    standardDeviation: Double,
    underlyingPrice: Double,
    plByPrice: Map<Int, Double>
  ): Double {
    return plByPrice.map { (price, pl) ->
      val diff = abs(underlyingPrice - price)
      val sdMultiple = if (diff < standardDeviation) {
        // Use a flat multiple for prices within 1SD to
        // avoid heavily favoring neutral strategies
        2.0
      } else {
        1.0 + (standardDeviation / diff)
      }

      pl * sdMultiple
    }.sum()
  }

}

data class Score(
  val pricePointScore: Double
)

data class RawScoredTrade(
  val score: Score,
  val plByPrice: Map<Int, Double>,
  val sdPrices: StandardDeviationPrices,
  val trade: Trade
)

data class ScoredTrade(
  val score: Int,
  val plByPrice: Map<Int, Double>,
  val sdPrices: StandardDeviationPrices,
  val trade: Trade
)

class StandardDeviationPrices(
  underlyingPrice: Double,
  private val standardDeviation: Double
) {

  val oneSdUp = underlyingPrice + standardDeviation
  val oneSdDown = underlyingPrice - standardDeviation
  val twoSdUp = underlyingPrice + (standardDeviation * 2)
  val twoSdDown = underlyingPrice - (standardDeviation * 2)
  val threeSdUp = underlyingPrice + (standardDeviation * 3)
  val threeSdDown = underlyingPrice - (standardDeviation * 3)

  override fun toString(): String {
    return "$standardDeviation [$threeSdDown - $threeSdUp]"
  }
}
