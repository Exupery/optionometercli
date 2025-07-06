package com.optionometer.output

import com.optionometer.screener.ScoredTrade
import com.optionometer.screener.Trade
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

  fun write(
    underlier: String,
    scoredTrades: List<List<ScoredTrade>>
  ) {
    logger.info("Outputting trades for $underlier to CSV(s) in $OUTPUT_FOLDER")
    val tradesByFilename = mapToFileNames(underlier, scoredTrades)
    tradesByFilename.forEach { (filename, trades) ->
      val file = File("$OUTPUT_FOLDER$filename")
      val fileWriter = FileWriter(file)
      val csvPrinter = CSVFormat.EXCEL.builder().get()
      printLines(trades, csvPrinter, fileWriter)
      fileWriter.flush()
      fileWriter.close()
      logger.info("${trades.size} trades written to ${file.name}")
    }
  }

  private fun printLines(
    trades: List<ScoredTrade>,
    csvPrinter: CSVFormat,
    fileWriter: FileWriter
  ) {
    csvPrinter.printRecord(
      fileWriter,
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

    trades.forEach { trade ->
      val profitLoss = trade.maxProfitLoss
      csvPrinter.printRecord(
        fileWriter,
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

  private fun Double.twoDigits(): String {
    return String.format("%.2f", this)
  }

  private fun tradeString(trade: Trade): String {
    val buys = trade.buys.map { "${it.side} ${it.strike}" }
    val sells = trade.sells.map { "${it.side} ${it.strike}" }
    return "BUY $buys - SELL $sells"
  }

  private fun mapToFileNames(
    underlier: String,
    scoredTrades: List<List<ScoredTrade>>
  ): Map<String, List<ScoredTrade>> {
    val withoutEmpties = scoredTrades.filter { it.isNotEmpty() }
    if (withoutEmpties.isEmpty()) {
      return emptyMap()
    }

    return withoutEmpties.associateBy { trades ->
      val trade = trades.first().trade
      val leg = (trade.buys + trade.sells).first()
      val instant = Instant.ofEpochSecond(leg.expiry)
      val date = DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(ZoneId.of("UTC")).format(instant)
      "$underlier$date.csv"
    }
  }

}
