package com.optionometer.main

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.optionometer.quotes.Importer
import com.optionometer.quotes.marketdata.MarketDataImporter
import com.optionometer.screener.Screener
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfiguration {

  @Bean
  fun objectMapper(): ObjectMapper {
    return ObjectMapper()
      .registerModule(KotlinModule.Builder().build())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  @Bean
  fun importer(
    @Value("\${marketdata.rootEndpoint}") rootEndpoint: String,
    objectMapper: ObjectMapper
  ): Importer {
    return MarketDataImporter(rootEndpoint, objectMapper)
  }

  @Bean
  fun screener(
    importer: Importer,
    @Value("\${screener.expiration.minDays}") minDays: Int,
    @Value("\${screener.expiration.maxDays}") maxDays: Int
  ): Screener {
    return Screener(importer, minDays, maxDays)
  }

}