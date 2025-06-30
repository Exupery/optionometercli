package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.models.OptionChain
import com.optionometer.models.Side
import com.optionometer.utils.call
import com.optionometer.utils.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private const val MIN_STRIKE = 20
private const val MAX_STRIKE = 30

class TradeBuilderTest {

  private val optionChain = optionChain()

  @Test
  fun `verify empty option chain is handled gracefully`() {
    val emptyChain = OptionChain("", 0.0, Instant.now(), emptyList(), emptyList())
    val builder = TradeBuilder(emptyChain)

    assertTrue(builder.spreads().isEmpty())
    assertTrue(builder.threeLegTrades().isEmpty())
    assertTrue(builder.fourLegTrades().isEmpty())
  }

  @Test
  fun `verify spreads are built correctly`() {
    val builder = TradeBuilder(optionChain)

    val spreads = builder.spreads()
    assertFalse(spreads.isEmpty())
    spreads.forEach { spread ->
      val buys = spread.buys
      val sells = spread.sells
      assertEquals(1, buys.size)
      assertEquals(1, sells.size)
      assertNotEquals(buys[0].strike, sells[0].strike)
    }
  }

  @Test
  fun `verify three leg trades are built correctly`() {
    val builder = TradeBuilder(optionChain)

    val trades = builder.threeLegTrades()
    assertFalse(trades.isEmpty())
    trades.forEach { trade ->
      val buys = trade.buys
      val sells = trade.sells
      assertFalse(buys.isEmpty())
      assertFalse(sells.isEmpty())
      assertNotEquals(buys.size, sells.size)
      assertEquals(3, buys.size + sells.size)
      val strikes = (buys.map { it.strike } + sells.map { it.strike }).toSet()
      assertNotEquals(1, strikes.size)
    }
  }

  @Test
  fun `verify enhanced trades are built correctly`() {
    val builder = TradeBuilder(optionChain)

    val threeLegTrades = builder.threeLegTrades()
    assertFalse(threeLegTrades.isEmpty())
    val enhancedTrades = builder.enhancedTrades(threeLegTrades)
    assertFalse(enhancedTrades.isEmpty())
    val numOptionsInChain = optionChain.calls.size + optionChain.puts.size
    val expectedSize = numOptionsInChain * threeLegTrades.size
    assertEquals(expectedSize, enhancedTrades.size)
    enhancedTrades.forEach { trade ->
      val buys = trade.buys
      val sells = trade.sells
      assertFalse(buys.isEmpty())
      assertFalse(sells.isEmpty())
      assertEquals(buys.size, sells.size)
      assertEquals(4, buys.size + sells.size)
      val strikes = (buys.map { it.strike } + sells.map { it.strike }).toSet()
      assertNotEquals(1, strikes.size)
    }
  }

  @Test
  fun `verify four leg trades are built correctly`() {
    val builder = TradeBuilder(optionChain)

    val trades = builder.fourLegTrades()
    assertFalse(trades.isEmpty())
    trades.forEach { trade ->
      val buys = trade.buys
      val sells = trade.sells
      assertFalse(buys.isEmpty())
      assertFalse(sells.isEmpty())
      assertEquals(buys.size, sells.size)
      assertEquals(4, buys.size + sells.size)
      val strikes = (buys.map { it.strike } + sells.map { it.strike }).toSet()
      assertEquals(4, strikes.size)
    }
  }

  @Test
  fun `verify condors are built correctly`() {
    val builder = TradeBuilder(optionChain)

    val trades = builder.condors()
    assertFalse(trades.isEmpty())
    trades.forEach { trade ->
      val buys = trade.buys
      val sells = trade.sells
      assertFalse(buys.isEmpty())
      assertFalse(sells.isEmpty())
      assertEquals(2, buys.size)
      assertEquals(2, sells.size)
      val allLegs = buys + sells
      assertEquals(2, allLegs.filter { it.side == Side.CALL }.size)
      assertEquals(2, allLegs.filter { it.side == Side.PUT }.size)
      val strikes = (buys.map { it.strike } + sells.map { it.strike }).toSet()
      assertEquals(4, strikes.size)
      assertTrue(buys[0].strike < sells[0].strike)
      assertTrue(sells[0].strike < sells[1].strike)
      assertTrue(sells[1].strike < buys[1].strike)
    }
  }

  @Test
  fun `verify bull put spreads are built correctly`() {
    val builder = TradeBuilder(optionChain)

    val trades = builder.bullPutSpreads()
    assertFalse(trades.isEmpty())
    trades.forEach { trade ->
      val buys = trade.buys
      val sells = trade.sells
      assertFalse(buys.isEmpty())
      assertFalse(sells.isEmpty())
      assertEquals(1, buys.size)
      assertEquals(1, sells.size)
      val longOption = buys.first()
      val shortOption = sells.first()
      assertTrue(longOption.side == Side.PUT)
      assertTrue(shortOption.side == Side.PUT)
      assertTrue(longOption.strike < shortOption.strike)
    }
  }

  private fun optionChain(): OptionChain {
    return OptionChain(
      "DIS",
      0.0,
      Instant.now(),
      buildSide(Side.CALL),
      buildSide(Side.PUT)
    )
  }

  private fun buildSide(side: Side): List<Option> {
    return (MIN_STRIKE..MAX_STRIKE).map { strike ->
      if (side == Side.CALL) {
        call(strike.toDouble(), 0.0, 0.0)
      } else {
        put(strike.toDouble(), 0.0, 0.0)
      }
    }
  }

}
