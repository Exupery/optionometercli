package com.optionometer.screener

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StandardDeviationPricesTest {

  @ParameterizedTest
  @MethodSource("sds")
  fun `three SD is calculated correctly`(
    underlyingPrice: Double,
    standardDeviation: Double,
    expected: Pair<Double, Double>
  ) {
    val sdp = StandardDeviationPrices(underlyingPrice, standardDeviation)
    assertEquals(expected.first, sdp.lowerSd, 0.01)
    assertEquals(expected.second, sdp.upperSd, 0.01)
  }

  @SuppressWarnings
  private fun sds(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(10.0, 1.0, Pair(8.0, 12.0)),
      Arguments.of(10.0, 2.0, Pair(6.0, 14.0)),
      Arguments.of(10.0, 3.0, Pair(4.0, 16.0))
    )
  }

  @ParameterizedTest
  @MethodSource("potentiallyNegativeSds")
  fun `potentially negative SDs return as zero`(
    underlyingPrice: Double,
    standardDeviation: Double,
    expectedSdUp: Double
  ) {
    val sdp = StandardDeviationPrices(underlyingPrice, standardDeviation)
    assertEquals(1.0, sdp.lowerSd, 0.01)
    assertEquals(expectedSdUp, sdp.upperSd, 0.01)
  }

  @SuppressWarnings
  private fun potentiallyNegativeSds(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(1.0, 3.5, 8.0),
      Arguments.of(1.0, 5.0, 11.0),
      Arguments.of(1.0, 20.0, 41.0)
    )
  }

  @ParameterizedTest
  @MethodSource("sdBands")
  fun `sd band is calculated correctly`(
    underlyingPrice: Double,
    standardDeviation: Double,
    price: Double,
    expected: Int
  ) {
    val sdp = StandardDeviationPrices(underlyingPrice, standardDeviation)
    assertEquals(expected, sdp.sdBand(price))
  }

  @SuppressWarnings
  private fun sdBands(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(10.0, 1.0, 10.0, 1),
      Arguments.of(10.0, 1.0, 9.01, 1),
      Arguments.of(10.0, 1.0, 10.99, 1),
      Arguments.of(10.0, 1.0, 9.0, 2),
      Arguments.of(10.0, 1.0, 11.0, 2),
      Arguments.of(10.0, 1.0, 8.01, 2),
      Arguments.of(10.0, 1.0, 11.59, 2),
      Arguments.of(10.0, 1.0, 7.01, 3),
      Arguments.of(10.0, 1.0, 12.99, 3),
      Arguments.of(10.0, 1.0, 7.0, 4),
      Arguments.of(10.0, 1.0, 13.00, 4),
      Arguments.of(50.0, 2.0, 45.0, 3),
      Arguments.of(50.0, 2.0, 55.0, 3),
      Arguments.of(4.0, 1.5, 3.0, 1),
      Arguments.of(4.0, 1.5, 2.50, 2),
      Arguments.of(4.0, 1.5, 1.0, 3),
      Arguments.of(4.0, 1.5, 0.0, 3)
    )
  }
}
