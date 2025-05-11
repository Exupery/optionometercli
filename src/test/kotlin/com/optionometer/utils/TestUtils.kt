package com.optionometer.utils

import com.optionometer.models.Option
import com.optionometer.models.Side
import java.time.Instant

fun call(strike: Double, bid: Double, ask: Double): Option {
  return option(
    strike,
    bid,
    ask,
    Side.CALL
  )
}

fun put(strike: Double, bid: Double, ask: Double): Option {
  return option(
    strike,
    bid,
    ask,
    Side.PUT
  )
}

private fun option(
  strike: Double,
  bid: Double,
  ask: Double,
  side: Side
): Option {
  return Option(
    "${side.name[0]}-$strike",
    strike,
    side,
    Instant.now().epochSecond,
    10,
    bid,
    ask,
    0.5,
    0.5
  )
}
