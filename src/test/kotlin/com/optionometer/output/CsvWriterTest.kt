package com.optionometer.output

import com.optionometer.models.Option
import com.optionometer.models.Side
import com.optionometer.screener.Trade
import com.optionometer.screener.scorers.ScoredTrade
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import java.time.Instant
import java.util.UUID

private const val OUTPUT_FOLDER = "./output/csv/"

class CsvWriterTest {

  private val csvWriter = CsvWriter()

  private val underlier = "TEST"

  @Test
  fun `verify write handles an empty list`() {
    assertDoesNotThrow {
      csvWriter.write(underlier, emptyList())
    }
  }

  @Test
  fun `verify write handles a list of empty lists`() {
    assertDoesNotThrow {
      csvWriter.write(underlier, listOf(emptyList(), emptyList(), emptyList()))
    }
  }

  @Test
  fun `verify write creates file using correct name`() {
    val option = mockk<Option>()
    every { option.expiry } returns Instant.EPOCH.epochSecond
    every { option.side } returns Side.CALL
    every { option.strike } returns 0.00
    val trade = Trade(listOf(option), emptyList())
    val scoredTrade = mockk<ScoredTrade>(relaxed = true)
    every { scoredTrade.trade } returns trade
    val uuid = UUID.randomUUID().toString()
    csvWriter.write("TEST$uuid", listOf(listOf(scoredTrade)))
    val file = File("$OUTPUT_FOLDER$underlier${uuid}19700101.csv")
    assertTrue(file.exists())
  }

}
