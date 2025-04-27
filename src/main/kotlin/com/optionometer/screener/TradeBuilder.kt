package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.models.OptionChain

class TradeBuilder(
  optionChain: OptionChain
) {

  private val spreads = buildSpreads(optionChain.calls, optionChain.calls) +
      buildSpreads(optionChain.puts, optionChain.puts) +
      buildSpreads(optionChain.calls, optionChain.puts) +
      buildSpreads(optionChain.puts, optionChain.calls)

  private val threeLegTrades = buildThreeLegTrades(optionChain.puts + optionChain.calls)

  private val fourLegTrades = buildFourLegTrades()

  fun getSpreads(): List<Trade> {
    return spreads
  }

  fun getThreeLegTrades(): List<Trade> {
    return threeLegTrades
  }

  fun getFourLegTrades(): List<Trade> {
    return fourLegTrades
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
