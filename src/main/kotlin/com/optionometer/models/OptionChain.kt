package com.optionometer.models

import java.time.Instant

data class OptionChain(
  val underlying: String,
  val underlyingPrice: Double,
  val expiry: Instant,
  val calls: List<Option>,
  val puts: List<Option>
)
