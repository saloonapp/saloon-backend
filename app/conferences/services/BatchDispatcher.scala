package conferences.services

import org.joda.time.DateTime

case class TimeChecker(val _valid: Boolean = true) {
  def isTime(time: String, precision: Int = 5): TimeChecker = {
    filter(_ => {
      val nums = time.split(":").map(_.toInt)
      Math.abs(new DateTime().getMinuteOfDay - (60*nums(0) + nums(1))) < precision
    })
  }
  def isWeekDay(day: Int): TimeChecker = {
    filter(_ => {
      new DateTime().getDayOfWeek == day
    })
  }
  def run(f: Unit => Unit): TimeChecker = {
    if(this._valid) { f() }
    this
  }

  private def filter(f: Unit => Boolean): TimeChecker = {
    if(this._valid) {
      try {
        if(f()){
          TimeChecker(true)
        } else {
          TimeChecker(false)
        }
      } catch {
        case _ => TimeChecker(false)
      }
    } else {
      TimeChecker(false)
    }
  }
}

object BatchDispatcher {
  def isTime(time: String): Option[Unit] = {
    None
  }

}
