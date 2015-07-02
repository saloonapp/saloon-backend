package backend.controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action { implicit req =>
    Ok(backend.views.html.index())
  }

}
