package com.optionometer.output

import com.optionometer.screener.Trade
import com.optionometer.screener.scorers.ScoredBullPut
import com.optionometer.screener.scorers.ScoredTrade
import org.apache.commons.csv.CSVFormat
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val OUTPUT_FOLDER = "./output/csv/"
private const val DATE_FORMAT = "yyyyMMdd"

class CsvWriter {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val csvPrinter = CSVFormat.EXCEL.builder().get()

  fun write(
    underlier: String,
    scoredByExpiry: List<List<Any>>
  ) {
    if (scoredByExpiry.isEmpty() || scoredByExpiry.all { it.isEmpty() }) {
      return
    }

    val nonEmpty = scoredByExpiry.filterNot { it.isEmpty() }
    val first = nonEmpty.first().first()
    val scoredByFilename = nonEmpty.associate { scoredTrades ->
      val csvScored = when (first) {
        is ScoredTrade -> CsvScoredTrade(scoredTrades as List<ScoredTrade>)
        is ScoredBullPut -> CsvScoredBullPut(scoredTrades as List<ScoredBullPut>)
        else -> throw IllegalArgumentException("Unexpected scored type $scoredByExpiry")
      }
      val filename = getFilename(underlier, csvScored)
      filename to csvScored
    }

    printCsvs(underlier, scoredByFilename)
  }

  private fun printCsvs(
    underlier: String,
    scoredByFilename: Map<String, CsvOutput>
  ) {
    scoredByFilename.forEach { (filename, csvOutput) ->
      printCsv(underlier, filename, csvOutput)
    }
  }

  private fun printCsv(
    underlier: String,
    filename: String,
    csvTrades: CsvOutput
  ) {
    logger.info("Outputting trades for $underlier to CSV(s) in $OUTPUT_FOLDER")
    val file = File("$OUTPUT_FOLDER$filename")
    val fileWriter = FileWriter(file)
    csvTrades.printHeaders(csvPrinter, fileWriter)
    csvTrades.printRows(csvPrinter, fileWriter)
    fileWriter.flush()
    fileWriter.close()
    logger.info("${csvTrades.size()} trades written to ${file.name}")
  }

  private fun getFilename(
    underlier: String,
    scoredTrades: CsvOutput
  ): String {
    val instant = Instant.ofEpochSecond(scoredTrades.expiry())
    val date = DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(ZoneId.of("UTC")).format(instant)

    return "$underlier$date.csv"
  }

  private interface CsvOutput {
    fun expiry(): Long
    fun size(): Int
    fun printHeaders(csvPrinter: CSVFormat, appendable: Appendable)
    fun printRows(csvPrinter: CSVFormat, appendable: Appendable)

    fun Double.twoDigits(): String {
      return String.format("%.2f", this)
    }

    fun tradeString(trade: Trade): String {
      val buys = trade.buys.map { "${it.side} ${it.strike}" }
      val sells = trade.sells.map { "${it.side} ${it.strike}" }
      return "BUY $buys - SELL $sells"
    }
  }

  private class CsvScoredTrade(
    val scored: List<ScoredTrade>
  ) : CsvOutput {
    override fun expiry(): Long {
      val trade = scored.first().trade
      return (trade.buys + trade.sells).first().expiry
    }

    override fun size(): Int {
      return scored.size
    }

    override fun printHeaders(csvPrinter: CSVFormat, appendable: Appendable) {
      csvPrinter.printRecord(
        appendable,
        "Score",
        "Probability",
        "Annualized",
        "100 Trades",
        "Max Profit",
        "Max Loss",
        "Max P/L Ratio",
        "-1SD",
        "+1SD",
        "Trade"
      )
    }

    override fun printRows(csvPrinter: CSVFormat, appendable: Appendable) {
      scored.forEach { trade ->
        val profitLoss = trade.maxProfitLoss
        csvPrinter.printRecord(
          appendable,
          trade.score,
          trade.successProbability.twoDigits(),
          trade.annualizedReturn.twoDigits(),
          trade.hundredTrades,
          profitLoss.maxProfit.twoDigits(),
          profitLoss.maxLoss.twoDigits(),
          profitLoss.maxProfitToMaxLossRatio.twoDigits(),
          trade.sdPrices.lowerSd.twoDigits(),
          trade.sdPrices.upperSd.twoDigits(),
          tradeString(trade.trade)
        )
      }
    }

  }

  private class CsvScoredBullPut(
    val scored: List<ScoredBullPut>
  ) : CsvOutput {
    override fun expiry(): Long {
      return scored.first().trade.sells.first().expiry
    }

    override fun size(): Int {
      return scored.size
    }

    override fun printHeaders(csvPrinter: CSVFormat, appendable: Appendable) {
      csvPrinter.printRecord(
        appendable,
        "Score",
        "Probability",
        "Annualized",
        "Max Profit",
        "Max Loss",
        "Contracts",
        "Trade"
      )
    }

    override fun printRows(csvPrinter: CSVFormat, appendable: Appendable) {
      scored.forEach { trade ->
        val profitLoss = trade.maxProfitLoss
        csvPrinter.printRecord(
          appendable,
          trade.score,
          trade.successProbability.twoDigits(),
          trade.annualizedReturn.twoDigits(),
          profitLoss.maxProfit.twoDigits(),
          profitLoss.maxLoss.twoDigits(),
          trade.numContracts,
          tradeString(trade.trade)
        )
      }
    }

  }

}
