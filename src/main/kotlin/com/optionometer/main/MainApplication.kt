package com.optionometer.main

import com.optionometer.screener.Mode
import com.optionometer.screener.Screener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.Duration
import java.time.Instant

private const val DEFAULT_TICKER = "QQQ"

@SpringBootApplication
class MainApplication(@Autowired val screener: Screener) : CommandLineRunner {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun run(vararg args: String?) {
    val start = Instant.now()
    val ticker = if (args.isNotEmpty()) {
      val arg1 = args[0]
      if (arg1.isNullOrBlank()) {
        throw IllegalArgumentException("Ticker symbol cannot be blank")
      }
      arg1.uppercase()
    } else {
      DEFAULT_TICKER
    }

    val arg2 = args.getOrNull(1)
    val screenerMode = if (arg2 != null) {
      Mode.valueOf(arg2)
    } else {
      Mode.STRATEGY_OPTIMIZER
    }

    logger.info("Optionometer started with ticker $ticker in mode $screenerMode")
    screener.screen(ticker, screenerMode)
    val duration = Duration.between(start, Instant.now())
    val durStr = "${duration.toMinutesPart()}m ${duration.toSecondsPart()}s"
    logger.info("Optionometer complete for $ticker after $durStr")
  }
}

fun main(args: Array<String>) {
  runApplication<MainApplication>(*args)
}
