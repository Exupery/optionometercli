package com.optionometer.main

import com.optionometer.quotes.Importer
import com.optionometer.quotes.marketdata.MarketDataClient
import com.optionometer.quotes.marketdata.MarketDataImporter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfiguration {

  @Bean
  fun importer(): Importer {
    return MarketDataImporter(MarketDataClient())
  }

}