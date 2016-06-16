package conferences.controllers

import play.api.mvc._

object Application extends Controller {

  def future = Action { implicit req =>
    Ok(conferences.views.html.conferenceList("future"))
  }
  def past = Action { implicit req =>
    Ok(conferences.views.html.conferenceList("past"))
  }
  def create = Action { implicit req =>
    Ok(conferences.views.html.conferenceForm())
  }
  def edit(id: String) = Action { implicit req =>
    Ok(conferences.views.html.conferenceForm())
  }

}
