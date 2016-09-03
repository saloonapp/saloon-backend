package common.views

import common.{Config, Utils}
import org.joda.time.LocalDate
import play.api.data.Field
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import play.api.libs.json.JsValue
import play.api.libs.json.Json

object Helpers {
  def isActive(call: play.api.mvc.Call)(implicit req: RequestHeader): Boolean = req.path == call.toString
  def hasBaseUrl(call: play.api.mvc.Call)(implicit req: RequestHeader): Boolean = req.path.startsWith(call.toString)
  def isActive(url: String)(implicit req: RequestHeader): Boolean = req.path == url
  def hasBaseUrl(url: String)(implicit req: RequestHeader): Boolean = req.path.startsWith(url)

  def isRequired(field: Field): Boolean = field.constraints.find { case (name, args) => name == "constraint.required" }.isDefined

  def getArg(args: Seq[(Symbol, String)], arg: String, default: String = ""): String = args.map { case (symbol, value) => (symbol.name, value) }.toMap.get(arg).getOrElse(default)

  def hasArg(args: Seq[(Symbol, String)], arg: String, expectedValue: String = ""): Boolean =
    args.map { case (symbol, value) => (symbol.name, value) }.toMap.get(arg).map { argValue =>
      argValue == "" || argValue == expectedValue
    }.getOrElse(false)

  def toHtmlArgs(args: Seq[(Symbol, String)], exclude: Seq[String] = Seq()): Html =
    Html(args
      .filter(e => !exclude.contains(e._1.name))
      .map { case (symbol, value) => symbol.name + "=\"" + value + "\"" }
      .mkString(" "))

  def strOpt(str: String): Option[String] = if (str.isEmpty()) None else Some(str)

  implicit class LocalDateImprovements(d: LocalDate) {
    def isToday(): Boolean = d.equals(new LocalDate())
    def isAfterToday(): Boolean = d.isAfter(new LocalDate())
    def isTodayOrAfter(): Boolean = d.isToday() || d.isAfterToday()
  }
}

object App {
  def isMobile()(implicit req: RequestHeader): Boolean = req.headers.getAll("User-Agent").exists(ua => ua.contains("Mobi") && !ua.contains("iPad"))
}

object repeatWithIndex {
  // from https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/views/helper/Helpers.scala
  def apply(field: play.api.data.Field, min: Int = 1)(fieldRenderer: (Int, play.api.data.Field) => Html): Seq[Html] = {
    val indexes = field.indexes match {
      case Nil => 0 until min
      case complete if complete.size >= min => field.indexes
      case partial =>
        // We don't have enough elements, append indexes starting from the largest
        val start = field.indexes.max + 1
        val needed = min - field.indexes.size
        field.indexes ++ (start until (start + needed))
    }

    indexes.map(i => fieldRenderer(i, field("[" + i + "]")))
  }
}

object Format {
  def jsonDiff(source: JsValue, destination: JsValue): Html = {
    val sourceLines: List[String] = Json.prettyPrint(source).split("\n").toList
    val destinationLines: List[String] = Json.prettyPrint(destination).split("\n").toList

    var offset = 0
    val resultLines = sourceLines.zipWithIndex.map {
      case (line, i) =>
        if (i + offset < destinationLines.length && line == destinationLines(i + offset)) {
          line
        } else if (i + offset < destinationLines.length && getKey(line) == getKey(destinationLines(i + offset))) {
          "<span style='font-weight: bold;'>" + line + "</span>"
        } else if (i + offset + 1 < destinationLines.length && getKey(line) == getKey(destinationLines(i + offset + 1))) {
          offset = offset + 1
          line
        } else {
          offset = offset - 1
          "<span style='color: green; font-weight: bold;'>" + line + "</span>"
        }
    }

    Html(resultLines.mkString("\n"))
  }
  val keyMatcher = " *\"([^\"]+)\" : .*".r
  private def getKey(line: String): String = {
    line match {
      case keyMatcher(key) => key
      case _ => ""
    }
  }

  def jsonDiff2(source: JsValue, destination: JsValue): Html = {
    val sourceLines: List[String] = Json.prettyPrint(source).split("\n").toList
    val destinationLines: List[String] = Json.prettyPrint(destination).split("\n").toList
    val resultLines = sourceLines.zip(destinationLines).map {
      case (src, dest) =>
        if (src == dest) src
        else "<b>" + src + "</b>"
    }
    Html(resultLines.mkString("\n"))
  }
}
