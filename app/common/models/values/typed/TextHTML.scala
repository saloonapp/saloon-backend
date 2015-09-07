package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper
import org.jsoup.Jsoup

case class TextHTML(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
  def toPlainText: TextMultiline = TextHTML.toPlainText(this)
}
object TextHTML extends tStringHelper[TextHTML] {
  def build(str: String): Either[String, TextHTML] = Right(TextHTML(str))
  def toPlainText(html: TextHTML): TextMultiline = TextMultiline(Jsoup.parse(html.unwrap).text())
}
