package backend.controllers

import play.api._
import play.api.mvc._

object Sample extends Controller {

  def index = Action { implicit req => Ok(backend.views.html.sample.index("index")) }
  def page(pageName: String) = Action { implicit req =>
    pageName match {
      case "index" => Ok(backend.views.html.sample.index(pageName))
      case "typography" => Ok(backend.views.html.sample.typography(pageName))
      case "widget-templates" => Ok(backend.views.html.sample.widgetTemplates(pageName))
      case "widgets" => Ok(backend.views.html.sample.widgets(pageName))
      case "tables" => Ok(backend.views.html.sample.tables(pageName))
      case "data-tables" => Ok(backend.views.html.sample.dataTables(pageName))
      case "form-elements" => Ok(backend.views.html.sample.formElements(pageName))
      case "form-components" => Ok(backend.views.html.sample.formComponents(pageName))
      case "form-examples" => Ok(backend.views.html.sample.formExamples(pageName))
      case "form-validations" => Ok(backend.views.html.sample.formValidations(pageName))
      case "colors" => Ok(backend.views.html.sample.colors(pageName))
      case "animations" => Ok(backend.views.html.sample.animations(pageName))
      case "box-shadow" => Ok(backend.views.html.sample.boxShadow(pageName))
      case "buttons" => Ok(backend.views.html.sample.buttons(pageName))
      case "icons" => Ok(backend.views.html.sample.icons(pageName))
      case "alerts" => Ok(backend.views.html.sample.alerts(pageName))
      case "notification-dialog" => Ok(backend.views.html.sample.notificationDialog(pageName))
      case "media" => Ok(backend.views.html.sample.media(pageName))
      case "components" => Ok(backend.views.html.sample.components(pageName))
      case "breadcrumbs" => Ok(backend.views.html.sample.breadcrumbs(pageName))
      case "other-components" => Ok(backend.views.html.sample.otherComponents(pageName))
      case "flot-charts" => Ok(backend.views.html.sample.flotCharts(pageName))
      case "other-charts" => Ok(backend.views.html.sample.otherCharts(pageName))
      case "calendar" => Ok(backend.views.html.sample.calendar(pageName))
      case "generic-classes" => Ok(backend.views.html.sample.genericClasses(pageName))
      case "profile-about" => Ok(backend.views.html.sample.profileAbout(pageName))
      case "profile-timeline" => Ok(backend.views.html.sample.profileTimeline(pageName))
      case "profile-photos" => Ok(backend.views.html.sample.profilePhotos(pageName))
      case "profile-connections" => Ok(backend.views.html.sample.profileConnections(pageName))
      case "list-view" => Ok(backend.views.html.sample.listView(pageName))
      case "messages" => Ok(backend.views.html.sample.messages(pageName))
      case "login" => Ok(backend.views.html.sample.login())
      case "404" => Ok(backend.views.html.sample.error404("Oups..."))
      case _ => Ok(backend.views.html.sample.index(pageName))
    }
  }

}
