package conferences.services

import org.joda.time.DateTime

case class TimeChecker(name: String, val _valid: Boolean = true) {
  def isTime(time: String, precision: Int = 5): TimeChecker = {
    filter(() => {
      val nums = time.split(":").map(_.toInt)
      Math.abs(new DateTime().getMinuteOfDay - (60*nums(0) + nums(1))) < precision
    })
  }
  def isWeekDay(day: Int): TimeChecker = { // see org.joda.time.DateTimeConstants
    filter(() => {
      new DateTime().getDayOfWeek == day
    })
  }
  def run(f: () => Unit): TimeChecker = {
    if(this._valid) { f() }
    this
  }

  private def filter(f: () => Boolean): TimeChecker = {
    if(this._valid) {
      try {
        if(f()){
          TimeChecker(this.name, true)
        } else {
          TimeChecker(this.name, false)
        }
      } catch {
        case _: Throwable => TimeChecker(this.name, false)
      }
    } else {
      TimeChecker(this.name, false)
    }
  }
}
