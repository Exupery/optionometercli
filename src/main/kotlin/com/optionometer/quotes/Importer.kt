package com.optionometer.quotes

interface Importer {

  fun fetchOptionChains(
    ticker: String,
    minDaysToExpiration: Int,
    maxDaysToExpiration: Int
  )

}