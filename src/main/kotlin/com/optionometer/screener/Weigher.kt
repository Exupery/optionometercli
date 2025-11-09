package com.optionometer.screener

class Weigher {

  private val pricePointWeight: Int = System.getProperty("PRICE_POINT_WEIGHT")?.toInt() ?: 1
  private val numProfitPointWeight: Int = System.getProperty("PROFIT_POINT_WEIGHT")?.toInt() ?: 1
  private val probabilityWeight: Int = System.getProperty("PROBABILITY_WEIGHT")?.toInt() ?: 1
  private val maxProfitLossWeight: Int = System.getProperty("PROFIT_LOSS_WEIGHT")?.toInt() ?: 1
  private val annualReturnWeight: Int = System.getProperty("ANNUAL_RETURN_WEIGHT")?.toInt() ?: 1
  private val deltaWeight: Int = System.getProperty("DELTA_WEIGHT")?.toInt() ?: 1
  private val hundredTradeWeight: Int = System.getProperty("HUNDRED_TRADE_WEIGHT")?.toInt() ?: 1

  fun scoreByWeight(
    pricePointScore: Double,
    numProfitPointScore: Double,
    probabilityScore: Double,
    maxProfitLossScore: Double,
    annualReturnScore: Double,
    deltaScore: Double,
    hundredTradesScore: Double
  ): Double {
    return (pricePointScore * pricePointWeight) +
        (numProfitPointScore * numProfitPointWeight) +
        (probabilityScore * probabilityWeight) +
        (maxProfitLossScore * maxProfitLossWeight) +
        (annualReturnScore * annualReturnWeight) +
        (deltaScore * deltaWeight) +
        (hundredTradesScore * hundredTradeWeight)
  }

  fun scoreByWeight(
    numProfitPointScore: Double,
    probabilityScore: Double,
    maxProfitLossScore: Double,
    annualReturnScore: Double
  ): Double {
    return (numProfitPointScore * numProfitPointWeight) +
        (probabilityScore * probabilityWeight) +
        (maxProfitLossScore * maxProfitLossWeight) +
        (annualReturnScore * annualReturnWeight)
  }

}
