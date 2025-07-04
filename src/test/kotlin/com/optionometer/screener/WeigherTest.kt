package com.optionometer.screener

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WeigherTest {

  private val baseScore = 60.0

  @BeforeEach
  fun setup() {
    System.setProperty("PRICE_POINT_WEIGHT", "1")
    System.setProperty("PROFIT_POINT_WEIGHT", "1")
    System.setProperty("PROBABILITY_WEIGHT", "1")
    System.setProperty("PROFIT_LOSS_WEIGHT", "1")
    System.setProperty("ANNUAL_RETURN_WEIGHT", "1")
    System.setProperty("DELTA_WEIGHT", "1")
    System.setProperty("HUNDRED_TRADE_WEIGHT", "1")
  }

  @Test
  fun `zero scores are gracefully handled`() {
    val weigher = Weigher()
    val actual = weigher.scoreByWeight(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    assertEquals(0.0, actual, 0.01)
  }

  @Test
  fun `zero weights are gracefully handled`() {
    System.setProperty("PRICE_POINT_WEIGHT", "0")
    System.setProperty("PROFIT_POINT_WEIGHT", "0")
    System.setProperty("PROBABILITY_WEIGHT", "0")
    System.setProperty("PROFIT_LOSS_WEIGHT", "0")
    System.setProperty("ANNUAL_RETURN_WEIGHT", "0")
    System.setProperty("DELTA_WEIGHT", "0")
    System.setProperty("HUNDRED_TRADE_WEIGHT", "0")
    val weigher = Weigher()
    val actual = weigher.scoreByWeight(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    assertEquals(0.0, actual, 0.01)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 5, 10, 47, 100])
  fun `price point score is properly weighted`(
    weight: Int
  ) {
    System.setProperty("PRICE_POINT_WEIGHT", weight.toString())
    val weigher = Weigher()
    val expected = baseScore + (10 * weight)
    val actual = weigher.scoreByWeight(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0)
    assertEquals(expected, actual, 0.1)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 5, 10, 47, 100])
  fun `profit points score is properly weighted`(
    weight: Int
  ) {
    System.setProperty("PROFIT_POINT_WEIGHT", weight.toString())
    val weigher = Weigher()
    val expected = baseScore + (10 * weight)
    val actual = weigher.scoreByWeight(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0)
    assertEquals(expected, actual, 0.1)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 5, 10, 47, 100])
  fun `probability score is properly weighted`(
    weight: Int
  ) {
    System.setProperty("PROBABILITY_WEIGHT", weight.toString())
    val weigher = Weigher()
    val expected = baseScore + (10 * weight)
    val actual = weigher.scoreByWeight(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0)
    assertEquals(expected, actual, 0.1)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 5, 10, 47, 100])
  fun `profit loss score is properly weighted`(
    weight: Int
  ) {
    System.setProperty("PROFIT_LOSS_WEIGHT", weight.toString())
    val weigher = Weigher()
    val expected = baseScore + (10 * weight)
    val actual = weigher.scoreByWeight(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0)
    assertEquals(expected, actual, 0.1)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 5, 10, 47, 100])
  fun `annual return score is properly weighted`(
    weight: Int
  ) {
    System.setProperty("ANNUAL_RETURN_WEIGHT", weight.toString())
    val weigher = Weigher()
    val expected = baseScore + (10 * weight)
    val actual = weigher.scoreByWeight(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0)
    assertEquals(expected, actual, 0.1)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 5, 10, 47, 100])
  fun `delta score is properly weighted`(
    weight: Int
  ) {
    System.setProperty("DELTA_WEIGHT", weight.toString())
    val weigher = Weigher()
    val expected = baseScore + (10 * weight)
    val actual = weigher.scoreByWeight(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0)
    assertEquals(expected, actual, 0.1)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 5, 10, 47, 100])
  fun `hundred trades score is properly weighted`(
    weight: Int
  ) {
    System.setProperty("HUNDRED_TRADE_WEIGHT", weight.toString())
    val weigher = Weigher()
    val expected = baseScore + (10 * weight)
    val actual = weigher.scoreByWeight(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0)
    assertEquals(expected, actual, 0.1)
  }

}
