package backend.views.Events.Sessions

import common.models.event.Session
import org.joda.time.DateTime

object ScheduleHelper {
  def hasTimeConflict(s1: Session, s2: Session): Boolean = {
    // s1.start = s2.start || s1.end = s2.end || (s1.start < s2.start < s1.end) || (s1.start < s2.end < s1.end) || (s2.start < s1.start && s1.end < s2.end)
    s1.uuid != s2.uuid && (
      s1.info.start.get.isEqual(s2.info.start.get) ||
      s1.info.end.get.isEqual(s2.info.end.get) ||
      (s1.info.start.get.isBefore(s2.info.start.get) && s2.info.start.get.isBefore(s1.info.end.get)) ||
      (s1.info.start.get.isBefore(s2.info.end.get) && s2.info.end.get.isBefore(s1.info.end.get)) ||
      (s2.info.start.get.isBefore(s1.info.start.get) && s1.info.end.get.isBefore(s2.info.end.get))
    )
  }
  def getConflicts(session: Session, allSessions: List[Session]): List[Session] = {
    allSessions.filter(s => hasTimeConflict(session, s))
  }
  def absolutePosition(session: Session, allSessions: List[Session], dayStart: DateTime, pxPerMinute: Int): String = {
    val startOffset = session.info.start.get.getMillis - dayStart.getMillis
    val duration = session.info.end.get.getMillis - session.info.start.get.getMillis
    val conflicts = allSessions.filter(s => hasTimeConflict(session, s) || session.uuid == s.uuid)
    val width = 100/conflicts.length
    val left = conflicts.indexOf(session) * width
    s"top: ${toPx(startOffset, pxPerMinute)}px; " +
    s"height: ${toPx(duration, pxPerMinute)}px; " +
    s"min-height: ${toPx(duration, pxPerMinute)}px; " +
    s"width: $width%; " +
    s"left: $left%;"
  }
  def toPx(durationMillis: Long, pxPerMinute: Int): Double = {
    durationMillis * pxPerMinute / (60*1000)
  }
}
