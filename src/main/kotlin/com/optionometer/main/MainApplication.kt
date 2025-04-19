package com.optionometer.main

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

private const val DEFAULT_TICKER = "QQQ"

@SpringBootApplication
class MainApplication : CommandLineRunner {
  override fun run(vararg args: String?) {
    val ticker = if (args.isNotEmpty()) {
      args[0]!!.uppercase()
    } else {
      DEFAULT_TICKER
    }
    println("Optionometer started with ticker $ticker")
  }
}

fun main(args: Array<String>) {
  runApplication<MainApplication>(*args)
}
