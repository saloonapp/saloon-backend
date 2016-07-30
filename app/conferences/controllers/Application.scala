package conferences.controllers

import play.api.mvc.{Controller, Action}

object Application extends Controller {
  def about = Action { implicit req =>
    Ok(conferences.views.html.about())
  }
}
