package com.optionometer.main

import com.optionometer.screener.Screener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

private const val DEFAULT_TICKER = "QQQ"

@SpringBootApplication
class MainApplication(@Autowired val screener: Screener) : CommandLineRunner {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun run(vararg args: String?) {
    val ticker = if (args.isNotEmpty()) {
      val arg1 = args[0]
      if (arg1.isNullOrBlank()) {
        throw IllegalArgumentException("Ticker symbol cannot be blank")
      }
      arg1.uppercase()
    } else {
      DEFAULT_TICKER
    }
    logger.info("Optionometer started with ticker $ticker")
    screener.screen(ticker)
  }
}

fun main(args: Array<String>) {
  runApplication<MainApplication>(*args)
}
