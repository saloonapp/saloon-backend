package backend.controllers

import common.models.user.User
import common.models.event.AttendeeRegistration
import common.models.event.EventConfigAttendeeSurvey
import common.models.event.EventConfigAttendeeSurveyQuestion
import common.repositories.user.OrganizationRepository
import common.repositories.event.EventRepository
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Ticketing extends SilhouetteEnvironment with ControllerHelpers {
  val configForm: Form[EventConfigAttendeeSurvey] = Form(EventConfigAttendeeSurvey.fields)
  val registerForm: Form[AttendeeRegistration] = Form(AttendeeRegistration.fields)

  def details(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      Future(Ok(backend.views.html.Events.Ticketing.details(event)))
    }
  }

  def configure(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      val form = event.config.attendeeSurvey.map(s => configForm.fill(s)).getOrElse(configForm)
      Future(Ok(backend.views.html.Events.Ticketing.configure(event, form)))
    }
  }

  def doConfigure(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      configForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(backend.views.html.Events.Ticketing.configure(event, formWithErrors))),
        formData => {
          // TODO : create specific method in EventRepository
          val eventWithCfg = event.copy(config = event.config.copy(attendeeSurvey = Some(formData)))
          EventRepository.update(eventId, eventWithCfg).map {
            _.map { eventUpdated =>
              Redirect(backend.controllers.routes.Ticketing.details(eventId)).flashing("success" -> s"Configuration bien prise en compte")
            }.getOrElse {
              InternalServerError(backend.views.html.Events.Ticketing.configure(event, configForm.fill(formData))).flashing("error" -> s"Problème lors de la mise à jour :(")
            }
          }
        })
    }
  }

  def doActivate(eventId: String, activated: Boolean) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      // TODO : create specific method in EventRepository.toggleOption("ticketing", activated)
      val eventWithCfg = event.copy(config = event.config.setTicketing(activated))
      EventRepository.update(eventId, eventWithCfg).map { eventUpdatedOpt =>
        if (activated) {
          Redirect(backend.controllers.routes.Ticketing.details(eventId)).flashing("success" -> "Ticketing activé !")
        } else {
          Redirect(backend.controllers.routes.Ticketing.details(eventId)).flashing("success" -> "Ticketing désactivé !")
        }
      }
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

  def register(eventId: String) = UserAwareAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      if (event.config.hasTicketing) {
        Future(Ok(backend.views.html.Events.Ticketing.register(registerForm.fill(AttendeeRegistration.prepare(event.config.attendeeSurvey.get)), event)))
      } else {
        Future(NotFound(backend.views.html.error("Oups", "No registration opened...")))
      }
    }
  }

  def doRegister(eventId: String) = UserAwareAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      registerForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(backend.views.html.Events.Ticketing.register(formWithErrors, event))),
        formData => Future(Ok(backend.views.html.Events.Ticketing.register(registerForm.fill(formData), event))))
    }
  }
}
