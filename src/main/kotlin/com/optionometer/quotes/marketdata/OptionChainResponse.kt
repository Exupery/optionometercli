package com.optionometer.quotes.marketdata

data class OptionChainResponse(
  val s: String,
  val optionSymbol: List<String>,
  val expiration: List<Long>,
  val side: List<String>,
  val strike: List<Int>,
  val dte: List<Int>,
  val bid: List<Double>,
  val ask: List<Double>,
  val intrinsicValue: List<Double>,
  val extrinsicValue: List<Double>,
  val underlyingPrice: List<Double>,
  val iv: List<Double>,
  val delta: List<Double>,
  val gamma: List<Double>,
  val theta: List<Double>,
  val vega: List<Double>
)
