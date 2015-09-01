package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.event.AttendeeRegistration
import common.models.event.EventConfigAttendeeSurvey
import common.models.event.EventConfigAttendeeSurveyQuestion
import common.repositories.user.OrganizationRepository
import common.repositories.event.EventRepository
import common.services.EventSrv
import backend.forms.EventCreateData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import com.mohiva.play.silhouette.core.LoginInfo

object Ticketing extends SilhouetteEnvironment {
  val configForm: Form[EventConfigAttendeeSurvey] = Form(EventConfigAttendeeSurvey.fields)
  val registerForm: Form[AttendeeRegistration] = Form(AttendeeRegistration.fields)

  def details(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(backend.views.html.Events.Ticketing.details(elt))
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def configure(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        val form = elt.config.attendeeSurvey.map(s => configForm.fill(s)).getOrElse(configForm)
        Ok(backend.views.html.Events.Ticketing.configure(elt, form))
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doConfigure(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).flatMap {
      _.map { elt =>
        configForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(backend.views.html.Events.Ticketing.configure(elt, formWithErrors))),
          formData => {
            val eventCfg = elt.copy(config = elt.config.copy(attendeeSurvey = Some(formData)))
            EventRepository.update(uuid, eventCfg).map {
              _.map { updatedElt =>
                Redirect(backend.controllers.routes.Ticketing.details(updatedElt.uuid)).flashing("success" -> s"Configuration bien prise en compte")
              }.getOrElse {
                InternalServerError(backend.views.html.Events.Ticketing.configure(elt, configForm.fill(formData))).flashing("error" -> s"Problème lors de la mise à jour :(")
              }
            }
          })
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def activate(uuid: String, activated: Boolean) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).flatMap {
      _.map { elt =>
        val eventCfg = elt.copy(config = elt.config.setTicketing(activated))
        EventRepository.update(uuid, eventCfg).map { eltOpt =>
          if (activated) {
            Redirect(backend.controllers.routes.Ticketing.details(uuid)).flashing("success" -> "Ticketing activé !")
          } else {
            Redirect(backend.controllers.routes.Ticketing.details(uuid)).flashing("error" -> "Ticketing désactivé !")
          }
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  /*
  val survey: EventConfigAttendeeSurvey = EventConfigAttendeeSurvey(List(), List(
    EventConfigAttendeeSurveyQuestion(multiple = false, required = false, otherAllowed = false, question = "Niveau de formation", answers = List("Bac / Bac+1", "Bac+2", "Bac+3", "Bac+4/5 et +")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Diplôme", answers = List("IADE", "IBODE", "IDE", "Masseur Kinésithérapeute", "Aide soignante", "Auxiliaire de périculture", "Certificat de capacité ambulancier", "Diplôme d'état de sage-femme", "Licence professionelle", "Auxiliaire de vie", "Responsable de structure d'enfance")),
    EventConfigAttendeeSurveyQuestion(multiple = false, required = false, otherAllowed = false, question = "Niveau d'expérience", answers = List("Jeune diplômé", "1 à 2 ans", "3 à 5 ans", "> 5 ans")),
    EventConfigAttendeeSurveyQuestion(multiple = false, required = false, otherAllowed = false, question = "Situation professionnelle actuelle", answers = List("En poste", "En recherche d'emploi")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = false, question = "Poste recherché", answers = List("CDD", "CDI", "Intérim", "Libérale")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Dernier poste occupé, poste actuel ou recherché", answers = List("IADE", "Kinésithérapeute", "Auxiliaire de périculture", "Manipulateur radio", "Infirmier d'entreprise", "Pédiatrie", "IBODE", "Cadre spécialisé", "Sage-femme", "Infirmier miliaire", "Puériculture", "Coordinatrice de crèche", "Etudiant IFSI", "IDE", "Auxiliaire de vie", "Educateur jeune enfant", "Aide - soignante", "Ergothérapeute", "Ambulancier", "Audioprothésiste", "Psychomotricien", "Orthoptiste", "Orthophoniste", "Ostéopathe", "Chriopracteur")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Secteur", answers = List("Etablissement privés", "Etablissements publics", "Libéral")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Comment avez-vous été informé de la tenue de ce salon ?", answers = List("les Métiers de la Petite Enfance", "Recrut.com", "A Nous Paris", "Le Marché du travail", "L'Aide Soignante", "Objectif Emploi", "La Revue de l'infirmière", "Soin spécial Emploi Salon", "L'Express", "Soins", "infirmier.com", "keljob.com", "letudiant.fr", "emploisoignant.fr", "cadresante.com", "lemarchedutravail.fr", "parisjob.com", "objectifemploi.fr", "aide-soignate.com", "actusoins.fr", "emploisante.com", "jobautonomie.com", "jobenfance.com", "jobintree.com", "capijobnew.com", "Panneau périphérique", "Pôle Emploi", "IFSI-CHU"))))
    */

  def register(uuid: String) = UserAwareAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        if (elt.config.hasTicketing) {
          Ok(backend.views.html.Events.Ticketing.register(registerForm.fill(AttendeeRegistration.prepare(elt.config.attendeeSurvey.get)), elt))
        } else {
          NotFound(backend.views.html.error("Oups", "No registration opened..."))
        }
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doRegister(uuid: String) = UserAwareAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        registerForm.bindFromRequest.fold(
          formWithErrors => BadRequest(backend.views.html.Events.Ticketing.register(formWithErrors, elt)),
          formData => Ok(backend.views.html.Events.Ticketing.register(registerForm.fill(formData), elt)))
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }
}
