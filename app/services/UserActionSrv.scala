package services

import models._

object UserActionSrv {
  def byItem(actions: List[UserActionFull]): List[(EventItem, List[UserActionFull])] = {
    actions.groupBy(_.item).toList.sortBy(e => -e._2.length)
  }

  def byAction(actions: List[UserActionFull]): List[(UserActionConent, List[UserActionFull])] = {
    actions.groupBy(_.action).toList
  }

  def byActionName(actions: List[UserActionFull]): List[(String, List[UserActionFull])] = {
    actions.groupBy(a => toActionName(a.action)).toList
  }

  def toActionName(a: UserActionConent): String = {
    a match {
      case FavoriteUserAction(favorite) => FavoriteUserAction.className
      case DoneUserAction(done) => DoneUserAction.className
      case MoodUserAction(rating, mood) => MoodUserAction.className
      case CommentUserAction(text, comment) => CommentUserAction.className
      case SubscribeUserAction(email, filter, subscribe) => SubscribeUserAction.className
      case _ => "unknown"
    }
  }
}