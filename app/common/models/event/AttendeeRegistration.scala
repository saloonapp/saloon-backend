package common.models.event

import play.api.data.Forms._

case class AttendeeRegistrationQuestion(question: String, multiple: Boolean, otherAllowed: Boolean, answers: List[String])
case class AttendeeRegistrationSurvey(questions: List[AttendeeRegistrationQuestion])
case class AttendeeRegistrationAnswerChecked(answer: String, checked: Boolean)
case class AttendeeRegistrationAnswer(question: String, answers: List[AttendeeRegistrationAnswerChecked], answer: String, multiple: Boolean, otherAllowed: Boolean, other: String)
case class AttendeeRegistration(
  genre: String,
  firstName: String,
  lastName: String,
  birthYear: Option[Int],
  email: String,
  phone: String,
  street: String,
  zipCode: String,
  city: String,
  answers: List[AttendeeRegistrationAnswer])
object AttendeeRegistration {
  val fields = mapping(
    "genre" -> nonEmptyText,
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "birthYear" -> optional(number),
    "email" -> email,
    "phone" -> text,
    "street" -> text,
    "zipCode" -> text,
    "city" -> text,
    "answers" -> list(mapping(
      "question" -> text,
      "answers" -> list(mapping(
        "label" -> text,
        "checked" -> boolean)(AttendeeRegistrationAnswerChecked.apply)(AttendeeRegistrationAnswerChecked.unapply)),
      "answer" -> text,
      "multiple" -> boolean,
      "otherAllowed" -> boolean,
      "other" -> text)(AttendeeRegistrationAnswer.apply)(AttendeeRegistrationAnswer.unapply)))(AttendeeRegistration.apply)(AttendeeRegistration.unapply)

  def prepare(survey: AttendeeRegistrationSurvey): AttendeeRegistration =
    AttendeeRegistration(
      "",
      "",
      "",
      None,
      "",
      "",
      "",
      "",
      "",
      survey.questions.map { q =>
        AttendeeRegistrationAnswer(
          q.question,
          q.answers.map(a => AttendeeRegistrationAnswerChecked(a, false)),
          "",
          q.multiple,
          q.otherAllowed,
          "")
      })
}