package services

object FileExporter {
  def makeCsv(elts: List[Map[String, String]]): String = {
    if (elts.isEmpty) {
      ""
    } else {
      val headers = elts.head.map { case (key, value) => key }.mkString(FileImporter.delimiter.toString) + FileImporter.eol.toString
      val values = elts.map { _.map { case (key, value) => value }.mkString(FileImporter.delimiter.toString) }.mkString(FileImporter.eol.toString)
      headers + values
    }
  }
}
