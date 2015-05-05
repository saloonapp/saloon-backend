package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def home = Action { implicit req =>
    Ok(views.html.Application.home())
  }
  def sample = Action { implicit req =>
    Ok(views.html.Application.sample())
  }

  def getEvents = TODO
  def getEvent(uuid: String) = TODO
  def getEventSessions(uuid: String) = TODO
  def getEventExponents(uuid: String) = TODO

  def findUser(deviceId: String) = TODO
  def getUser(uuid: String) = TODO

}
