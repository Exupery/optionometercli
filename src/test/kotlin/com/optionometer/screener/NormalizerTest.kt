package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.screener.scorers.MaxProfitLoss
import com.optionometer.screener.scorers.RawScoredTrade
import com.optionometer.screener.scorers.Score
import com.optionometer.screener.scorers.StandardDeviationPrices
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NormalizerTest {

  private val testStandardDeviationPrices = StandardDeviationPrices(100.0, 10.0)

  @Test
  fun `verify normalize handles an empty list`() {
    assertDoesNotThrow {
      val scored = Normalizer.normalize(emptyList())
      assertTrue(scored.isEmpty())
    }
  }

  @ParameterizedTest
  @MethodSource("scores")
  fun `verify normalize scores profit & loss by price`(
    plByPriceScores: List<Double>,
    expected: List<Int>
  ) {
    val rawScoredTrades = plByPriceScores.map { plByPriceScore ->
      rawScoredTrade(plByPriceScore = plByPriceScore)
    }

    verify(rawScoredTrades, expected)
  }

  @ParameterizedTest
  @MethodSource("scores")
  fun `verify normalize scores number of profitable price points`(
    numProfitablePointsScores: List<Double>,
    expected: List<Int>
  ) {
    val rawScoredTrades = numProfitablePointsScores.map { numProfitablePointsScore ->
      rawScoredTrade(numProfitablePointsScore = numProfitablePointsScore)
    }

    verify(rawScoredTrades, expected)
  }

  @ParameterizedTest
  @MethodSource("scores")
  fun `verify normalize scores number of success probability`(
    probabilityScores: List<Double>,
    expected: List<Int>
  ) {
    val rawScoredTrades = probabilityScores.map { probabilityScore ->
      rawScoredTrade(probabilityScore = probabilityScore)
    }

    verify(rawScoredTrades, expected)
  }

  @ParameterizedTest
  @MethodSource("scores")
  fun `verify normalize scores profit loss ratio`(
    maxProfitLossScores: List<Double>,
    expected: List<Int>
  ) {
    val rawScoredTrades = maxProfitLossScores.map { maxProfitLossScore ->
      rawScoredTrade(maxProfitToMaxLossRatio = maxProfitLossScore)
    }

    verify(rawScoredTrades, expected)
  }

  @ParameterizedTest
  @MethodSource("scores")
  fun `verify normalize scores by annualized return`(
    annualizedReturns: List<Double>,
    expected: List<Int>
  ) {
    val rawScoredTrades = annualizedReturns.map { annualizedReturn ->
      rawScoredTrade(annualizedReturn = annualizedReturn)
    }

    verify(rawScoredTrades, expected)
  }

  @ParameterizedTest
  @MethodSource("scores")
  fun `verify normalize scores by deltas`(
    deltas: List<Double>,
    expected: List<Int>
  ) {
    val rawScoredTrades = deltas.map { delta ->
      rawScoredTrade(delta = delta)
    }

    verify(rawScoredTrades, expected)
  }

  @ParameterizedTest
  @MethodSource("scores")
  fun `verify normalize scores by hundred trades`(
    hundredTradesScores: List<Double>,
    expected: List<Int>
  ) {
    val rawScoredTrades = hundredTradesScores.map { hundredTradeScore ->
      rawScoredTrade(hundredTradesScore = (hundredTradeScore * 100).toInt())
    }

    verify(rawScoredTrades, expected)
  }

  private fun verify(
    rawScoredTrades: List<RawScoredTrade>,
    expected: List<Int>
  ) {
    val scored = Normalizer.normalize(rawScoredTrades)
    assertFalse(scored.isEmpty())
    assertEquals(expected.size, scored.size)
    scored.map { it.score }.withIndex().forEach { indexedValue ->
      assertEquals(expected[indexedValue.index], indexedValue.value)
    }
  }

  @SuppressWarnings
  private fun scores(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(listOf(0.0), listOf(0)),
      Arguments.of(listOf(0.0, 0.1), listOf(0, 100)),
      Arguments.of(listOf(0.0, Double.MAX_VALUE), listOf(0, 100)),
      Arguments.of(listOf(Double.MAX_VALUE - 1, Double.MAX_VALUE), listOf(0, 100)),
      Arguments.of(listOf(1.0, 2.0, 3.0, 4.0, 5.0), listOf(0, 25, 50, 75, 100)),
      Arguments.of((0..100).map { it.toDouble() }, (0..100).toList()),
      Arguments.of((0..500).map { it.toDouble() }, (0..500).withIndex().map { (it.index * 0.2).toInt() }),
    )
  }

  private fun rawScoredTrade(
    plByPriceScore: Double = 0.0,
    numProfitablePointsScore: Double = 0.0,
    probabilityScore: Double = 0.0,
    maxProfitToMaxLossRatio: Double = 0.0,
    annualizedReturn: Double = 0.0,
    delta: Double = 0.0,
    hundredTradesScore: Int = 0
  ): RawScoredTrade {
    val score = Score(
      plByPriceScore,
      numProfitablePointsScore,
      probabilityScore,
      maxProfitToMaxLossRatio,
      annualizedReturn,
      delta,
      hundredTradesScore
    )
    val option = mockk<Option>(relaxed = true)
    every { option.symbol } returns UUID.randomUUID().toString()
    val trade = Trade(listOf(option), emptyList())
    val maxProfitLoss = MaxProfitLoss(
      maxProfitToMaxLossRatio,
      5,
      100.0,
      5,
      100.0 * (maxProfitToMaxLossRatio / 100),
      maxProfitToMaxLossRatio
    )
    return RawScoredTrade(score, emptyMap(), testStandardDeviationPrices, maxProfitLoss, delta, trade)
  }
}
