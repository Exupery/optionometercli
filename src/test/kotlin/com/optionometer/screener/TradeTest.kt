package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.models.Side
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TradeTest {

  private val underlying = "DIS"

  @Test
  fun `verify no options has zero profit`() {
    val trade = Trade(
      underlying,
      100.00,
      emptyList(),
      emptyList()
    )

    val profit = trade.profitLossAtPrice(200.00)
    assertEquals(0.00, profit, 0.01)
  }

  @ParameterizedTest
  @MethodSource("longCalls")
  fun `verify long call profit`(
    strike: Double,
    bid: Double,
    ask: Double,
    target: Double,
    expected: Double
  ) {
    val longCall = call(strike, bid, ask)
    val trade = Trade(underlying, 0.0, listOf(longCall), emptyList())
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  private fun longCalls(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(10.0, 1.5, 1.6, 8.0, -1.6),
      Arguments.of(10.0, 1.5, 1.6, 9.0, -1.6),
      Arguments.of(10.0, 1.5, 1.6, 10.0, -1.6),
      Arguments.of(10.0, 1.5, 1.6, 11.0, -0.6),
      Arguments.of(10.0, 1.5, 1.6, 12.0, 0.4),
      Arguments.of(10.0, 1.5, 1.6, 13.0, 1.4)
    )
  }

  @ParameterizedTest
  @MethodSource("longPuts")
  fun `verify long put profit`(
    strike: Double,
    bid: Double,
    ask: Double,
    target: Double,
    expected: Double
  ) {
    val longPut = put(strike, bid, ask)
    val trade = Trade(underlying, 0.0, listOf(longPut), emptyList())
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  private fun longPuts(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(10.0, 1.5, 1.6, 7.0, 1.4),
      Arguments.of(10.0, 1.5, 1.6, 8.0, 0.4),
      Arguments.of(10.0, 1.5, 1.6, 9.0, -0.6),
      Arguments.of(10.0, 1.5, 1.6, 10.0, -1.6),
      Arguments.of(10.0, 1.5, 1.6, 11.0, -1.6),
      Arguments.of(10.0, 1.5, 1.6, 12.0, -1.6)
    )
  }

  @ParameterizedTest
  @MethodSource("shortCalls")
  fun `verify short call profit`(
    strike: Double,
    bid: Double,
    ask: Double,
    target: Double,
    expected: Double
  ) {
    val shortCall = call(strike, bid, ask)
    val trade = Trade(underlying, 0.0, emptyList(), listOf(shortCall))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  private fun shortCalls(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(10.0, 1.5, 1.6, 8.0, 1.5),
      Arguments.of(10.0, 1.5, 1.6, 9.0, 1.5),
      Arguments.of(10.0, 1.5, 1.6, 10.0, 1.5),
      Arguments.of(10.0, 1.5, 1.6, 11.0, 0.5),
      Arguments.of(10.0, 1.5, 1.6, 12.0, -0.5),
      Arguments.of(10.0, 1.5, 1.6, 13.0, -1.5),
      Arguments.of(10.0, 1.5, 1.6, 14.0, -2.5)
    )
  }

  @ParameterizedTest
  @MethodSource("shortPuts")
  fun `verify short put profit`(
    strike: Double,
    bid: Double,
    ask: Double,
    target: Double,
    expected: Double
  ) {
    val shortPut = put(strike, bid, ask)
    val trade = Trade(underlying, 0.0, emptyList(), listOf(shortPut))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  private fun shortPuts(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(10.0, 1.5, 1.6, 6.0, -2.5),
      Arguments.of(10.0, 1.5, 1.6, 7.0, -1.5),
      Arguments.of(10.0, 1.5, 1.6, 8.0, -0.5),
      Arguments.of(10.0, 1.5, 1.6, 9.0, 0.5),
      Arguments.of(10.0, 1.5, 1.6, 10.0, 1.5),
      Arguments.of(10.0, 1.5, 1.6, 11.0, 1.5),
      Arguments.of(10.0, 1.5, 1.6, 12.0, 1.5),
      Arguments.of(10.0, 1.5, 1.6, 13.0, 1.5)
    )
  }

  private fun call(strike: Double, bid: Double, ask: Double): Option {
    return option(
      strike,
      bid,
      ask,
      Side.CALL
    )
  }

  private fun put(strike: Double, bid: Double, ask: Double): Option {
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
      UUID.randomUUID().toString(),
      strike,
      side,
      Instant.now().epochSecond,
      0,
      bid,
      ask,
      0.0,
      0.0,
      0.0,
      0.0,
      0.0,
    )
  }

}