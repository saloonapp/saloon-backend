package common.models.utils

import play.api.data.validation.{Valid, ValidationError, Invalid, Constraint}

object Forms {
  object Constraints {
    def required[T]: Constraint[List[T]] = Constraint[List[T]]("constraint.required") { o =>
      if(o == null) Invalid(ValidationError("error.required")) else if(o.length == 0) Invalid(ValidationError("error.required")) else Valid
    }
  }
}
