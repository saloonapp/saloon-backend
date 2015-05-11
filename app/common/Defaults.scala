package common

import com.github.tototoshi.csv._

object Defaults {
  implicit object csvFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }
}