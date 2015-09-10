package tools.utils

object HTMLMatcher {
  // see regex options : http://www.expreg.com/options.php
  private def opt(regex: String): String = "(?:" + regex + ")?"
  val quote = "(?:'|\")"
  val tag = "<[^<>]+>"
  val tagEnd = "</[^<>]+>"
  val tagOpt = opt(tag)
  val tagEndOpt = opt(tagEnd)

  def simple(content: String, regex: String): List[Option[String]] = {
    val matcher = regex.r.unanchored
    content match {
      case matcher(val1, val2, val3, val4) => List(toOpt(val1), toOpt(val2), toOpt(val3), toOpt(val4))
      case matcher(val1, val2, val3) => List(toOpt(val1), toOpt(val2), toOpt(val3))
      case matcher(val1, val2) => List(toOpt(val1), toOpt(val2))
      case matcher(val1) => List(toOpt(val1))
      case _ => List(None, None, None, None)
    }
  }

  def multi(content: String, regex: String): List[List[Option[String]]] = {
    val matcher = regex.r.unanchored
    matcher.findAllIn(content).map {
      case matcher(val1, val2, val3, val4) => List(toOpt(val1), toOpt(val2), toOpt(val3), toOpt(val4))
      case matcher(val1, val2, val3) => List(toOpt(val1), toOpt(val2), toOpt(val3))
      case matcher(val1, val2) => List(toOpt(val1), toOpt(val2))
      case matcher(val1) => List(toOpt(val1))
      case _ => List(None, None, None, None)
    }.toList
  }

  private def isEmpty(str: String): Boolean = str == null || str.trim().isEmpty()
  private def trim(str: String): String = if (str != null) str.trim() else null
  private def toOpt(str: String): Option[String] = if (isEmpty(str)) None else Some(trim(str))
}