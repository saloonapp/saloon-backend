package services

import models.EventItem
import models.Session
import models.Exponent
import models.User
import models.UserAction
import models.UserActionConent
import models.FavoriteUserAction
import models.DoneUserAction
import models.MoodUserAction
import models.CommentUserAction
import models.SubscribeUserAction
import models.UserActionFull

object UserActionSrv {
  def byItem(actions: List[UserActionFull]): List[(EventItem, List[UserActionFull])] = {
    actions.groupBy(_.item).toList.sortBy(e => -e._2.length)
  }

  def byUser(actions: List[UserActionFull]): List[(User, List[UserActionFull])] = {
    actions.groupBy(_.user).toList.sortBy(e => -e._2.length)
  }

  def byAction(actions: List[UserActionFull]): List[(UserActionConent, List[UserActionFull])] = {
    actions.groupBy(_.action).toList
  }

  def byActionName(actions: List[UserActionFull]): List[(String, List[UserActionFull])] = {
    actions.groupBy(a => toActionName(a.action)).toList
  }

  def byMood(actions: List[UserActionFull]): List[(String, List[UserActionFull])] = {
    actions.filter(_.action.isMood()).groupBy(_.action match {
      case MoodUserAction(rating, mood) => rating
      case _ => "unknown"
    }).toList
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

  def favoriteSessions(actions: List[UserAction], sessions: List[Session]): List[(Session, Option[String], List[String])] = {
    actions.filter(a => a.action.isFavorite() && a.itemType == Session.className).map { a =>
      sessions.find(_.uuid == a.itemId)
    }.flatten
      .filter(_.format != "break")
      .map { s => (s, moodFor(actions, Session.className, s.uuid), commentsFor(actions, Session.className, s.uuid)) }
      .sortBy(r => -moodSort(r._2))
  }
  def notFavoriteSessions(actions: List[UserAction], sessions: List[Session]): List[(Session, Option[String], List[String])] = {
    val favoriteUuids = actions.filter(a => a.action.isFavorite() && a.itemType == Session.className).map(_.itemId)
    sessions
      .filter(s => !favoriteUuids.contains(s.uuid) && s.format != "break")
      .map { s => (s, moodFor(actions, Session.className, s.uuid), commentsFor(actions, Session.className, s.uuid)) }
      .sortBy(r => -moodSort(r._2))
  }
  def seenExponents(actions: List[UserAction], exponents: List[Exponent]): List[(Exponent, Option[String], List[String])] = {
    actions.filter(a => a.action.isDone() && a.itemType == Exponent.className).map { a =>
      exponents.find(_.uuid == a.itemId)
    }.flatten
      .map { e => (e, moodFor(actions, Exponent.className, e.uuid), commentsFor(actions, Exponent.className, e.uuid)) }
      .sortBy(r => -moodSort(r._2))
  }
  def notSeenExponents(actions: List[UserAction], exponents: List[Exponent]): List[(Exponent, Option[String], List[String])] = {
    val seenUuids = actions.filter(a => a.action.isDone() && a.itemType == Exponent.className).map(_.itemId)
    exponents
      .filter(e => !seenUuids.contains(e.uuid))
      .map { e => (e, moodFor(actions, Exponent.className, e.uuid), commentsFor(actions, Exponent.className, e.uuid)) }
      .sortBy(r => -moodSort(r._2))
  }
  def favoriteNotSeenExponents(actions: List[UserAction], exponents: List[Exponent]): List[(Exponent, Option[String], List[String])] = {
    val seenUuids = actions.filter(a => a.action.isDone() && a.itemType == Exponent.className).map(_.itemId)
    actions.filter(a => a.action.isFavorite() && a.itemType == Exponent.className && !seenUuids.contains(a.itemId)).map { a =>
      exponents.find(_.uuid == a.itemId)
    }.flatten
      .map { e => (e, moodFor(actions, Exponent.className, e.uuid), commentsFor(actions, Exponent.className, e.uuid)) }
      .sortBy(r => -moodSort(r._2))
  }
  def hasFavorites(actions: List[UserAction]): Boolean = actions.filter(_.action.isFavorite()).length > 0

  def listOpt[A](list: List[A]): Option[List[A]] = if (list.length > 0) Some(list) else None

  private def moodSort(m: Option[String]): Int = m.map {
    _ match {
      case "ok" => 3
      case "bof" => 2
      case "ko" => 1
      case _ => 0
    }
  }.getOrElse(0)
  private def moodFor(actions: List[UserAction], itemType: String, itemId: String): Option[String] = {
    actions.find(a => a.action.isMood() && a.itemType == itemType && a.itemId == itemId).flatMap {
      _.action match {
        case MoodUserAction(rating, mood) => Some(rating)
        case _ => None
      }
    }
  }
  private def commentsFor(actions: List[UserAction], itemType: String, itemId: String): List[String] = {
    actions.filter(a => a.action.isComment() && a.itemType == itemType && a.itemId == itemId).map {
      _.action match {
        case CommentUserAction(text, comment) => Some(text)
        case _ => None
      }
    }.flatten
  }
}