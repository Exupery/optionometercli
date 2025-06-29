package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.models.OptionChain
import kotlin.math.min

class TradeBuilder(
  private val optionChain: OptionChain
) {

  private val spreads by lazy {
    buildSpreads(optionChain.calls, optionChain.calls) +
        buildSpreads(optionChain.puts, optionChain.puts) +
        buildSpreads(optionChain.calls, optionChain.puts) +
        buildSpreads(optionChain.puts, optionChain.calls)
  }

  private val threeLegTrades by lazy {
    buildThreeLegTrades(optionChain.puts + optionChain.calls)
  }

  private val fourLegTrades by lazy {
    buildFourLegTrades()
  }

  val underlyingPrice = optionChain.underlyingPrice

  fun spreads(): List<Trade> {
    return spreads
  }

  fun threeLegTrades(): List<Trade> {
    return threeLegTrades
  }

  fun enhancedTrades(trades: List<Trade>): List<Trade> {
    return trades.flatMap { trade ->
      (optionChain.calls + optionChain.puts).map { option ->
        // Only add a leg to the buy/sell side with fewer legs
        if (trade.buys.size < trade.sells.size) {
          Trade(trade.buys + option, trade.sells)
        } else {
          Trade(trade.buys, trade.sells + option)
        }
      }
    }
  }

  fun fourLegTrades(): List<Trade> {
    return fourLegTrades
  }

  /**
   * Long PUT
   * Short PUT
   * Short CALL
   * Long CALL
   */
  fun condors(): List<Trade> {
    val puts = optionChain.puts.sortedBy { it.strike }
    val calls = optionChain.calls.sortedBy { it.strike }
    val maxIdx = min(puts.size, calls.size)

    val putPairs = (0..maxIdx - 4).map { idx ->
      val longPut = puts[idx]
      (idx + 1..maxIdx - 2).map { idx2 ->
        Pair(longPut, puts[idx2])
      }
    }.flatten()

    val callPairs = (2..maxIdx - 1).map { idx ->
      val longCall = calls[idx]
      (idx + 1..maxIdx - 1).map { idx2 ->
        Pair(longCall, calls[idx2])
      }
    }.flatten()

    return putPairs.map { p ->
      val longPut = p.first
      val shortPut = p.second
      callPairs.map { c ->
        val shortCall = c.first
        val longCall = c.second
        Trade(listOf(longPut, longCall), listOf(shortPut, shortCall))
      }
    }.flatten().filterNot { it.sells[0].strike >= it.sells[1].strike }
  }

  private fun buildSpreads(
    sells: List<Option>,
    buys: List<Option>
  ): List<Trade> {
    return sells.flatMap { sell ->
      buys.filter { it.strike != sell.strike }.map { buy ->
        Trade(listOf(buy), listOf(sell))
      }
    }
  }

  private fun buildThreeLegTrades(
    thirdLegs: List<Option>
  ): List<Trade> {
    val sells = spreads.flatMap { spread ->
      thirdLegs.map { third ->
        Trade(spread.buys, spread.sells + listOf(third))
      }
    }

    val buys = spreads.flatMap { spread ->
      thirdLegs.map { third ->
        Trade(spread.buys + listOf(third), spread.sells)
      }
    }

    return buys + sells
  }

  private fun buildFourLegTrades(): List<Trade> {
    return spreads.flatMap { baseSpread ->
      spreads.filter { newSpread ->
        newSpread.buys[0].strike != baseSpread.buys[0].strike &&
            newSpread.buys[0].strike != baseSpread.sells[0].strike &&
            newSpread.sells[0].strike != baseSpread.buys[0].strike &&
            newSpread.sells[0].strike != baseSpread.sells[0].strike
      }.map { spread ->
        Trade(baseSpread.buys + spread.buys, baseSpread.sells + spread.sells)
      }
    }

  }

}
