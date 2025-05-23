package com.optionometer.models

data class Option(
  val symbol: String,
  val strike: Double,
  val side: Side,
  val expiry: Long,
  val dte: Int,
  val bid: Double,
  val ask: Double,
  val impliedVolatility: Double,
  val delta: Double
)

enum class Side {
  CALL,
  PUT
}
