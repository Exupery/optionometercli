package com.optionometer.screener

import com.optionometer.models.Option
import com.optionometer.models.Side

class Trade(
  val buys: List<Option>,
  val sells: List<Option>
) {

  private val callBuys = buys.filter { it.side == Side.CALL }
  private val putBuys = buys.filter { it.side == Side.PUT }
  private val callSells = sells.filter { it.side == Side.CALL }
  private val putSells = sells.filter { it.side == Side.PUT }

  fun profitLossAtPrice(target: Double): Double {
    val callBuysPl = callBuys.sumOf { callBuyPl(it, target) }
    val callSellsPl = callSells.sumOf { callSellPl(it, target) }
    val putBuysPl = putBuys.sumOf { putBuyPl(it, target) }
    val putSellsPl = putSells.sumOf { putSellPl(it, target) }

    return callBuysPl + callSellsPl + putBuysPl + putSellsPl
  }

  private fun callBuyPl(call: Option, target: Double): Double {
    return if (call.strike > target) {
      call.ask * -1
    } else {
      target - call.strike - call.ask
    }
  }

  private fun callSellPl(call: Option, target: Double): Double {
    return if (call.strike > target) {
      call.bid
    } else {
      ((target - call.strike) * -1) + call.bid
    }
  }

  private fun putBuyPl(put: Option, target: Double): Double {
    return if (put.strike < target) {
      put.ask * -1
    } else {
      put.strike - target - put.ask
    }
  }

  private fun putSellPl(put: Option, target: Double): Double {
    return if (put.strike < target) {
      put.bid
    } else {
      ((put.strike - target) * -1) + put.bid
    }
  }

  override fun toString(): String {
    val buySymbols = buys.map { it.symbol }
    val sellSymbols = sells.map { it.symbol }
    return "BUY to open $buySymbols, SELL to open $sellSymbols"
  }

}
