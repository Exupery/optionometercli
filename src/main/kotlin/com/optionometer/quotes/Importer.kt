package com.optionometer.quotes

import com.optionometer.models.OptionChain

interface Importer {

  fun fetchOptionChains(
    ticker: String,
    minDaysToExpiration: Int,
    maxDaysToExpiration: Int
  ): List<OptionChain>

}