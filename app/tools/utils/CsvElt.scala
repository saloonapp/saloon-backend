package tools.utils

trait CsvElt {
  def toCsv(): Map[String, String]
}