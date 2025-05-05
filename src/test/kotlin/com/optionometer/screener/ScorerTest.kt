package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.utils.call
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScorerTest {

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

    val actual = callPrivateScore(unprofitableTrade, 10.0)
    assertNull(actual)
  }

  @Test
  fun `scores pl by price points`() {
    val option = mockk<Option>()
    every { option.impliedVolatility } returns 0.5
    every { option.dte } returns 365
    val trade = mockk<Trade>()
    every { trade.buys } returns listOf(option)
    every { trade.sells } returns emptyList()
    every { trade.profitLossAtPrice(any()) } returns 1.0
    // TODO COMPARE TO MORE PROFITABLE TRADE

    val actual = callPrivateScore(trade, 10.0)
    assertNotNull(actual)
    assertTrue(actual!!.score.pricePointScore > 0)
  }

  private fun callPrivateScore(trade: Trade, underlyingPrice: Double): RawScoredTrade? {
    return Scorer::class.declaredFunctions
      .first { it.name == "score" && it.visibility == KVisibility.PRIVATE }
      .apply { isAccessible = true }
      .call(Scorer, trade, underlyingPrice) as RawScoredTrade?
  }
}
