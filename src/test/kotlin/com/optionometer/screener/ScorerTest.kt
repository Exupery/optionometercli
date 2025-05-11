package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.utils.call
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScorerTest {

  private val underlyingPrice = 10.0
  private val call = mockk<Option>()
  private val put = mockk<Option>()
  private val smallProfit = mockk<Trade>()
  private val largeProfit = mockk<Trade>()

  @BeforeEach
  fun setup() {
    every { call.impliedVolatility } returns 0.5
    every { call.dte } returns 365
    every { put.impliedVolatility } returns 0.5
    every { put.dte } returns 365
    every { smallProfit.buys } returns listOf(call)
    every { smallProfit.sells } returns emptyList()
    every { smallProfit.profitLossAtPrice(any()) } returns -1.0
    every { smallProfit.profitLossAtPrice(underlyingPrice) } returns 100.0
    every { smallProfit.profitLossAtPrice(underlyingPrice + 1.0) } returns 100.0
    every { largeProfit.buys } returns emptyList()
    every { largeProfit.sells } returns listOf(call)
    every { largeProfit.profitLossAtPrice(any()) } returns 200.0
    every { largeProfit.profitLossAtPrice(underlyingPrice) } returns -1.0
    every { largeProfit.profitLossAtPrice(underlyingPrice - 1.0) } returns -1.0
  }

  @Test
  fun `empty list is gracefully handled`() {
    val scored = Scorer.score(emptyList(), 0.0)
    assertTrue(scored.isEmpty())
  }

  @Test
  fun `score returns null if trade is unprofitable at all prices`() {
    val option = call(10.0, 1.0, 2.0)
    val unprofitableTrade = mockk<Trade>(relaxed = true)
    every { unprofitableTrade.buys } returns listOf(option)
    every { unprofitableTrade.profitLossAtPrice(any()) } returns -1.0

    val actual = callPrivateScore(unprofitableTrade)
    assertNull(actual)
  }

  @Test
  fun `scores pl by price points`() {
    val smallProfitScore = callPrivateScore(smallProfit)
    assertNotNull(smallProfitScore)
    assertTrue(smallProfitScore!!.score.pricePointScore > 0)
    val largeProfitScore = callPrivateScore(largeProfit)
    assertNotNull(largeProfitScore)
    assertTrue(largeProfitScore!!.score.pricePointScore > smallProfitScore.score.pricePointScore)
  }

  @Test
  fun `scores by number of profitable price points`() {
    val smallProfitScore = callPrivateScore(smallProfit)
    assertNotNull(smallProfitScore)
    assertTrue(smallProfitScore!!.score.numProfitablePointsScore > 0)
    val largeProfitScore = callPrivateScore(largeProfit)
    assertNotNull(largeProfitScore)
    assertTrue(largeProfitScore!!.score.numProfitablePointsScore > smallProfitScore.score.numProfitablePointsScore)
  }

  @Test
  fun `scores by probability of profit`() {
    val smallProfitScore = callPrivateScore(smallProfit)
    assertNotNull(smallProfitScore)
    assertTrue(smallProfitScore!!.score.scoreByProbability > 0)
    val largeProfitScore = callPrivateScore(largeProfit)
    assertNotNull(largeProfitScore)
    assertTrue(largeProfitScore!!.score.scoreByProbability > smallProfitScore.score.scoreByProbability)
  }

  @Test
  fun `scores by max profit loss ratio`() {
    val smallProfitScore = callPrivateScore(smallProfit)
    assertNotNull(smallProfitScore)
    assertTrue(smallProfitScore!!.score.maxProfitToMaxLossRatio > 0)
    val largeProfitScore = callPrivateScore(largeProfit)
    assertNotNull(largeProfitScore)
    assertTrue(largeProfitScore!!.score.maxProfitToMaxLossRatio > smallProfitScore.score.maxProfitToMaxLossRatio)
  }

  @Test
  fun `scores by annualized return`() {
    val smallProfitScore = callPrivateScore(smallProfit)
    assertNotNull(smallProfitScore)
    assertTrue(smallProfitScore!!.score.annualizedReturn > 0)
    val largeProfitScore = callPrivateScore(largeProfit)
    assertNotNull(largeProfitScore)
    assertTrue(largeProfitScore!!.score.annualizedReturn > smallProfitScore.score.annualizedReturn)
  }

  private fun callPrivateScore(trade: Trade): RawScoredTrade? {
    return Scorer::class.declaredFunctions
      .first { it.name == "score" && it.visibility == KVisibility.PRIVATE }
      .apply { isAccessible = true }
      .call(Scorer, trade, underlyingPrice) as RawScoredTrade?
  }
}
