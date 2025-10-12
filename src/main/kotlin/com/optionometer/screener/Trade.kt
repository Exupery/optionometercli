package com.optionometer.screener

import com.optionometer.main.Properties
import com.optionometer.models.Option
import com.optionometer.models.Side
import kotlin.math.abs
import kotlin.math.min

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
    val askPlusCommission = call.ask + Properties.commissionPerShare
    return if (call.strike > target) {
      askPlusCommission * -1
    } else {
      target - call.strike - askPlusCommission
    }
  }

  private fun callSellPl(call: Option, target: Double): Double {
    val bidMinusCommission = call.bid - Properties.commissionPerShare
    return if (call.strike > target) {
      bidMinusCommission
    } else {
      ((target - call.strike) * -1) + bidMinusCommission
    }
  }

  private fun putBuyPl(put: Option, target: Double): Double {
    val askPlusCommission = put.ask + Properties.commissionPerShare
    return if (put.strike < target) {
      askPlusCommission * -1
    } else {
      put.strike - target - askPlusCommission
    }
  }

  private fun putSellPl(put: Option, target: Double): Double {
    val bidMinusCommission = put.bid - Properties.commissionPerShare
    return if (put.strike < target) {
      bidMinusCommission
    } else {
      ((put.strike - target) * -1) + bidMinusCommission
    }
  }

  fun requiredMargin(): Double {
    return calculateMargin(callBuys, callSells) + calculateMargin(putBuys, putSells)
  }

  private fun calculateMargin(
    buys: List<Option>,
    sells: List<Option>
  ): Double {
    if (buys.isEmpty() && sells.isEmpty()) {
      return 0.0
    }

    if ((buys + sells).groupBy { it.side }.size > 1) {
      throw IllegalArgumentException("Buys and Calls passed to this function must all be same side")
    }

    if (sells.isEmpty()) {
      return buys.sumOf { it.ask } * 100
    }
    if (buys.isEmpty()) {
      return sells.sumOf { (it.strike - it.bid) * 100 }
    }

    val buysSorted = buys.sortedBy { it.strike }
    val sellsSorted = sells.sortedBy { it.strike }
    return if (buysSorted.size == sellsSorted.size) {
      buysSorted.withIndex().sumOf { indexedValue ->
        calculateMargin(indexedValue.value, sellsSorted[indexedValue.index])
      }
    } else {
      val size = min(buysSorted.size, sellsSorted.size)
      val pairedMargin = calculateMargin(buysSorted.take(size), sellsSorted.take(size))
      val remaining = if (buysSorted.size > size) {
        calculateMargin(buysSorted.takeLast(buysSorted.size - size), emptyList())
      } else {
        calculateMargin(emptyList(), sellsSorted.takeLast(sellsSorted.size - size))
      }

      pairedMargin + remaining
    }
  }

  private fun calculateMargin(
    buy: Option,
    sell: Option
  ): Double {
    val buyStrike = buy.strike
    val buyAsk = buy.ask
    val sellStrike = sell.strike
    val sellBid = sell.bid

    return if (buyAsk >= sellBid) {
      buyAsk - sellBid
    } else {
      val diff = abs(buyStrike - sellStrike)
      diff - (sellBid - buyAsk)
    } * 100
  }

  override fun toString(): String {
    val buySymbols = buys.map { it.symbol }
    val sellSymbols = sells.map { it.symbol }
    return "BUY to open $buySymbols, SELL to open $sellSymbols"
  }

}
