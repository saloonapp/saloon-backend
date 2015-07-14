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

object Events extends SilhouetteEnvironment {
  val createForm: Form[EventCreateData] = Form(EventCreateData.fields)
  val registerForm: Form[AttendeeRegistration] = Form(AttendeeRegistration.fields)

  def list = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.findAll(sort = "-info.start").flatMap { events =>
      val eventsNullFirst = events.filter(_.info.start.isEmpty) ++ events.filter(_.info.start.isDefined)
      EventSrv.addMetadata(eventsNullFirst).map { fullEvents =>
        Ok(backend.views.html.Events.list(fullEvents.toList))
      }
    }
  }

  def details(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(uuid).flatMap {
      _.map { elt =>
        for {
          (event, attendeeCount, sessionCount, exponentCount, actionCount) <- EventSrv.addMetadata(elt)
        } yield {
          Ok(backend.views.html.Events.details(event, attendeeCount, sessionCount, exponentCount, actionCount))
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def create = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      organizations <- OrganizationRepository.findAll()
      categories <- EventRepository.getCategories()
    } yield {
      if (user.canAdministrateSaloon() || user.organizationId.isDefined) {
        Ok(backend.views.html.Events.create(createForm, organizations, categories))
      } else {
        Redirect(backend.controllers.routes.Events.list()).flashing("error" -> s"Vous ne pouvez pas créer un événement !")
      }
    }
  }

  def doCreate = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    createForm.bindFromRequest.fold(
      formWithErrors => for {
        organizations <- OrganizationRepository.findAll()
        categories <- EventRepository.getCategories()
      } yield BadRequest(backend.views.html.Events.create(formWithErrors, organizations, categories)),
      formData => EventRepository.insert(EventCreateData.toModel(formData)).flatMap {
        _.map { elt =>
          Future(Redirect(backend.controllers.routes.Events.details(elt.uuid)).flashing("success" -> s"Événement '${elt.name}' créé !"))
        }.getOrElse {
          for {
            organizations <- OrganizationRepository.findAll()
            categories <- EventRepository.getCategories()
          } yield InternalServerError(backend.views.html.Events.create(createForm.fill(formData), organizations, categories)).flashing("error" -> s"Impossible de créer l'événement '${formData.name}'")
        }
      })
  }

  def update(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eltOpt <- EventRepository.getByUuid(uuid)
      organizations <- OrganizationRepository.findAll()
      categories <- EventRepository.getCategories()
    } yield {
      eltOpt.map { elt =>
        Ok(backend.views.html.Events.update(createForm.fill(EventCreateData.fromModel(elt)), elt, organizations, categories))
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doUpdate(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(uuid).flatMap {
      _.map { elt =>
        createForm.bindFromRequest.fold(
          formWithErrors => for {
            organizations <- OrganizationRepository.findAll()
            categories <- EventRepository.getCategories()
          } yield BadRequest(backend.views.html.Events.update(formWithErrors, elt, organizations, categories)),
          formData => EventRepository.update(uuid, EventCreateData.merge(elt, formData)).flatMap {
            _.map { updatedElt =>
              Future(Redirect(backend.controllers.routes.Events.details(updatedElt.uuid)).flashing("success" -> s"L'événement '${updatedElt.name}' a bien été modifié"))
            }.getOrElse {
              for {
                organizations <- OrganizationRepository.findAll()
                categories <- EventRepository.getCategories()
              } yield InternalServerError(backend.views.html.Events.update(createForm.fill(formData), elt, organizations, categories)).flashing("error" -> s"Impossible de modifier l'événement '${elt.name}'")
            }
          })
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def delete(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        EventRepository.delete(uuid)
        Redirect(backend.controllers.routes.Events.list()).flashing("success" -> s"Suppression de l'événement '${elt.name}'")
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  val survey: EventConfigAttendeeSurvey = EventConfigAttendeeSurvey(List(), List(
    EventConfigAttendeeSurveyQuestion(multiple = false, required = false, otherAllowed = false, question = "Niveau de formation", answers = List("Bac / Bac+1", "Bac+2", "Bac+3", "Bac+4/5 et +")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Diplôme", answers = List("IADE", "IBODE", "IDE", "Masseur Kinésithérapeute", "Aide soignante", "Auxiliaire de périculture", "Certificat de capacité ambulancier", "Diplôme d'état de sage-femme", "Licence professionelle", "Auxiliaire de vie", "Responsable de structure d'enfance")),
    EventConfigAttendeeSurveyQuestion(multiple = false, required = false, otherAllowed = false, question = "Niveau d'expérience", answers = List("Jeune diplômé", "1 à 2 ans", "3 à 5 ans", "> 5 ans")),
    EventConfigAttendeeSurveyQuestion(multiple = false, required = false, otherAllowed = false, question = "Situation professionnelle actuelle", answers = List("En poste", "En recherche d'emploi")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = false, question = "Poste recherché", answers = List("CDD", "CDI", "Intérim", "Libérale")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Dernier poste occupé, poste actuel ou recherché", answers = List("IADE", "Kinésithérapeute", "Auxiliaire de périculture", "Manipulateur radio", "Infirmier d'entreprise", "Pédiatrie", "IBODE", "Cadre spécialisé", "Sage-femme", "Infirmier miliaire", "Puériculture", "Coordinatrice de crèche", "Etudiant IFSI", "IDE", "Auxiliaire de vie", "Educateur jeune enfant", "Aide - soignante", "Ergothérapeute", "Ambulancier", "Audioprothésiste", "Psychomotricien", "Orthoptiste", "Orthophoniste", "Ostéopathe", "Chriopracteur")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Secteur", answers = List("Etablissement privés", "Etablissements publics", "Libéral")),
    EventConfigAttendeeSurveyQuestion(multiple = true, required = false, otherAllowed = true, question = "Comment avez-vous été informé de la tenue de ce salon ?", answers = List("les Métiers de la Petite Enfance", "Recrut.com", "A Nous Paris", "Le Marché du travail", "L'Aide Soignante", "Objectif Emploi", "La Revue de l'infirmière", "Soin spécial Emploi Salon", "L'Express", "Soins", "infirmier.com", "keljob.com", "letudiant.fr", "emploisoignant.fr", "cadresante.com", "lemarchedutravail.fr", "parisjob.com", "objectifemploi.fr", "aide-soignate.com", "actusoins.fr", "emploisante.com", "jobautonomie.com", "jobenfance.com", "jobintree.com", "capijobnew.com", "Panneau périphérique", "Pôle Emploi", "IFSI-CHU"))))

  def register(uuid: String) = UserAwareAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(backend.views.html.Events.register(registerForm.fill(AttendeeRegistration.prepare(survey)), elt))
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doRegister(uuid: String) = UserAwareAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        registerForm.bindFromRequest.fold(
          formWithErrors => BadRequest(backend.views.html.Events.register(formWithErrors, elt)),
          formData => Ok(backend.views.html.Events.register(registerForm.fill(formData), elt)))
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }
}
