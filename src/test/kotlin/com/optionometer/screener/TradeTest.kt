package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.utils.call
import com.optionometer.utils.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TradeTest {

  @Test
  fun `verify no options in trade returns zero profit`() {
    val trade = Trade(
      emptyList(),
      emptyList()
    )

    val profit = trade.profitLossAtPrice(200.00)
    assertEquals(0.00, profit, 0.01)
  }

  @ParameterizedTest
  @MethodSource("requiredMargins")
  fun `verify requiredMargin is calculated correctly`(
    buys: List<Option>,
    sells: List<Option>,
    expected: Double
  ) {
    val trade = Trade(buys, sells)
    val actual = trade.requiredMargin()
    assertEquals(expected * 100, actual, 0.1)
  }

  @SuppressWarnings
  private fun requiredMargins(): Stream<Arguments> {
    val call110 = call(110.0, 6.0, 7.0)
    val call115 = call(115.0, 4.0, 5.0)
    val call120 = call(120.0, 2.0, 3.0)
    val call125 = call(125.0, 0.5, 1.0)
    val put110 = put(110.0, 2.0, 3.0)
    val put115 = put(115.0, 4.0, 5.0)
    val put120 = put(120.0, 6.0, 7.0)
    val put125 = put(125.0, 8.0, 9.0)
    return Stream.of(
      Arguments.of(listOf(call110), emptyList<Option>(), 7.0),
      Arguments.of(listOf(call110, call115), emptyList<Option>(), 12.0),
      Arguments.of(listOf(call115, call120, call125), emptyList<Option>(), 9.0),
      Arguments.of(emptyList<Option>(), listOf(put110), 110 - 2.0),
      Arguments.of(emptyList<Option>(), listOf(put110, put115), (110 - 2) + (115 - 4)),
      Arguments.of(listOf(call110), listOf(put115), (115 - 4) + 7),
      Arguments.of(listOf(call115), listOf(put110), (110 - 2) + 5),
      Arguments.of(listOf(put120), listOf(put125), 4),
      Arguments.of(listOf(put115), listOf(put125), 7),
      Arguments.of(listOf(put125), listOf(put120), 3),
      Arguments.of(listOf(put125), listOf(put115), 5),
      Arguments.of(listOf(call115), listOf(call120), 3),
      Arguments.of(listOf(call115), listOf(call120, put125), 117 + 3),
      Arguments.of(listOf(call115), listOf(call120, call125), 124.5 + 3),
      Arguments.of(listOf(put110, call125), listOf(put115, call120), 8),
      Arguments.of(listOf(put115, call120), listOf(put110, call125), 5.5)
    )
  }

  /* *********************
   * SINGLE LEG STRATEGIES
   ********************* */

  @ParameterizedTest
  @MethodSource("longCalls")
  fun `verify long call profit`(
    longCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longCall), emptyList())
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun longCalls(): Stream<Arguments> {
    val longCall = call(10.0, 1.5, 1.6)
    return Stream.of(
      Arguments.of(longCall, 8.0, -1.6),
      Arguments.of(longCall, 9.0, -1.6),
      Arguments.of(longCall, 10.0, -1.6),
      Arguments.of(longCall, 11.0, -0.6),
      Arguments.of(longCall, 12.0, 0.4),
      Arguments.of(longCall, 13.0, 1.4)
    )
  }

  @ParameterizedTest
  @MethodSource("longPuts")
  fun `verify long put profit`(
    longPut: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longPut), emptyList())
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun longPuts(): Stream<Arguments> {
    val longPut = put(10.0, 1.5, 1.6)
    return Stream.of(
      Arguments.of(longPut, 7.0, 1.4),
      Arguments.of(longPut, 8.0, 0.4),
      Arguments.of(longPut, 9.0, -0.6),
      Arguments.of(longPut, 10.0, -1.6),
      Arguments.of(longPut, 11.0, -1.6),
      Arguments.of(longPut, 12.0, -1.6)
    )
  }

  @ParameterizedTest
  @MethodSource("shortCalls")
  fun `verify short call profit`(
    shortCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(emptyList(), listOf(shortCall))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun shortCalls(): Stream<Arguments> {
    val shortCall = call(10.0, 1.5, 1.6)
    return Stream.of(
      Arguments.of(shortCall, 8.0, 1.5),
      Arguments.of(shortCall, 9.0, 1.5),
      Arguments.of(shortCall, 10.0, 1.5),
      Arguments.of(shortCall, 11.0, 0.5),
      Arguments.of(shortCall, 12.0, -0.5),
      Arguments.of(shortCall, 13.0, -1.5),
      Arguments.of(shortCall, 14.0, -2.5)
    )
  }

  @ParameterizedTest
  @MethodSource("shortPuts")
  fun `verify short put profit`(
    shortPut: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(emptyList(), listOf(shortPut))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun shortPuts(): Stream<Arguments> {
    val shortPut = put(10.0, 1.5, 1.6)
    return Stream.of(
      Arguments.of(shortPut, 6.0, -2.5),
      Arguments.of(shortPut, 7.0, -1.5),
      Arguments.of(shortPut, 8.0, -0.5),
      Arguments.of(shortPut, 9.0, 0.5),
      Arguments.of(shortPut, 10.0, 1.5),
      Arguments.of(shortPut, 11.0, 1.5),
      Arguments.of(shortPut, 12.0, 1.5)
    )
  }

  /* *********************
   * TWO LEG STRATEGIES
   ********************* */

  @ParameterizedTest
  @MethodSource("longCallSpreads")
  fun `verify long call spread profit`(
    longCall: Option,
    shortCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longCall), listOf(shortCall))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun longCallSpreads(): Stream<Arguments> {
    val longCall = call(10.0, 1.5, 1.6)
    val shortCall = call(12.0, 1.1, 1.2)
    return Stream.of(
      Arguments.of(longCall, shortCall, 8.0, -0.5),
      Arguments.of(longCall, shortCall, 9.0, -0.5),
      Arguments.of(longCall, shortCall, 10.0, -0.5),
      Arguments.of(longCall, shortCall, 10.5, 0.0),
      Arguments.of(longCall, shortCall, 11.0, 0.5),
      Arguments.of(longCall, shortCall, 12.0, 1.5),
      Arguments.of(longCall, shortCall, 13.0, 1.5),
      Arguments.of(longCall, shortCall, 14.0, 1.5)
    )
  }

  @ParameterizedTest
  @MethodSource("longPutSpreads")
  fun `verify long put spread profit`(
    shortPut: Option,
    longPut: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longPut), listOf(shortPut))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun longPutSpreads(): Stream<Arguments> {
    val shortPut = put(10.0, 1.1, 1.2)
    val longPut = put(12.0, 1.5, 1.6)
    return Stream.of(
      Arguments.of(shortPut, longPut, 8.0, 1.5),
      Arguments.of(shortPut, longPut, 9.0, 1.5),
      Arguments.of(shortPut, longPut, 10.0, 1.5),
      Arguments.of(shortPut, longPut, 11.0, 0.5),
      Arguments.of(shortPut, longPut, 11.5, 0.0),
      Arguments.of(shortPut, longPut, 12.0, -0.5),
      Arguments.of(shortPut, longPut, 13.0, -0.5),
      Arguments.of(shortPut, longPut, 14.0, -0.5)
    )
  }

  @ParameterizedTest
  @MethodSource("shortCallSpreads")
  fun `verify short call spread profit`(
    shortCall: Option,
    longCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longCall), listOf(shortCall))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun shortCallSpreads(): Stream<Arguments> {
    val shortCall = call(10.0, 1.5, 1.6)
    val longCall = call(12.0, 1.1, 1.2)
    return Stream.of(
      Arguments.of(shortCall, longCall, 8.0, 0.3),
      Arguments.of(shortCall, longCall, 9.0, 0.3),
      Arguments.of(shortCall, longCall, 10.0, 0.3),
      Arguments.of(shortCall, longCall, 10.3, 0.0),
      Arguments.of(shortCall, longCall, 11.0, -0.7),
      Arguments.of(shortCall, longCall, 12.0, -1.7),
      Arguments.of(shortCall, longCall, 13.0, -1.7)
    )
  }

  @ParameterizedTest
  @MethodSource("shortPutSpreads")
  fun `verify short put spread profit`(
    longPut: Option,
    shortPut: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longPut), listOf(shortPut))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun shortPutSpreads(): Stream<Arguments> {
    val longPut = put(10.0, 1.1, 1.2)
    val shortPut = put(12.0, 1.5, 1.6)
    return Stream.of(
      Arguments.of(longPut, shortPut, 8.0, -1.7),
      Arguments.of(longPut, shortPut, 9.0, -1.7),
      Arguments.of(longPut, shortPut, 10.0, -1.7),
      Arguments.of(longPut, shortPut, 11.0, -0.7),
      Arguments.of(longPut, shortPut, 11.7, 0.0),
      Arguments.of(longPut, shortPut, 12.0, 0.3),
      Arguments.of(longPut, shortPut, 13.0, 0.3),
      Arguments.of(longPut, shortPut, 14.0, 0.3)
    )
  }

  @ParameterizedTest
  @MethodSource("longStraddles")
  fun `verify long straddle profit`(
    longPut: Option,
    longCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longPut, longCall), emptyList())
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun longStraddles(): List<Arguments> {
    val strike = 11.0
    val longPut = put(strike, 1.3, 1.4)
    val longCall = call(strike, 1.5, 1.6)
    return (5..17).map { target ->
      val expected = abs(strike - target) - (longPut.ask + longCall.ask)
      Arguments.of(longPut, longCall, target, expected)
    }
  }

  @ParameterizedTest
  @MethodSource("shortStraddles")
  fun `verify short straddle profit`(
    shortPut: Option,
    shortCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(emptyList(), listOf(shortPut, shortCall))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun shortStraddles(): List<Arguments> {
    val strike = 11.0
    val shortPut = put(strike, 1.3, 1.4)
    val shortCall = call(strike, 1.5, 1.6)
    return (5..17).map { target ->
      val expected = (abs(strike - target) * -1) + (shortPut.bid + shortCall.bid)
      Arguments.of(shortPut, shortCall, target, expected)
    }
  }

  @ParameterizedTest
  @MethodSource("longStrangles")
  fun `verify long strangle profit`(
    longPut: Option,
    longCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(listOf(longPut, longCall), emptyList())
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun longStrangles(): List<Arguments> {
    val longPut = put(10.0, 1.3, 1.4)
    val longCall = call(12.0, 1.5, 1.6)
    return (4..18).map { target ->
      val cost = longPut.ask + longCall.ask
      val expected = if (target < longPut.strike) {
        longPut.strike - target - cost
      } else if (target > longCall.strike) {
        target - longCall.strike - cost
      } else {
        cost * -1
      }
      Arguments.of(longPut, longCall, target, expected)
    }
  }

  @ParameterizedTest
  @MethodSource("shortStrangles")
  fun `verify short strangle profit`(
    shortPut: Option,
    shortCall: Option,
    target: Double,
    expected: Double
  ) {
    val trade = Trade(emptyList(), listOf(shortPut, shortCall))
    val actual = trade.profitLossAtPrice(target)
    assertEquals(expected, actual, 0.01)
  }

  @SuppressWarnings
  private fun shortStrangles(): List<Arguments> {
    val shortPut = put(10.0, 1.3, 1.4)
    val shortCall = call(12.0, 1.5, 1.6)
    return (4..18).map { target ->
      val credit = shortPut.bid + shortCall.bid
      val expected = if (target < shortPut.strike) {
        target - shortPut.strike + credit
      } else if (target > shortCall.strike) {
        shortCall.strike - target + credit
      } else {
        credit
      }
      Arguments.of(shortPut, shortCall, target, expected)
    }
  }

}
