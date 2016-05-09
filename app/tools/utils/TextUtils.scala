package tools.utils

object TextUtils {
  def tokenify(str: String): String = {
    str.trim.toLowerCase.replace(" ", "-")
  }
}
