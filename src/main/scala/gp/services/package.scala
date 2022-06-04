package gp

import gp.utils.routing.errors.{ApiError, ApiErrorLike}

package object services {

  object errors {

    sealed trait ServiceError extends ApiErrorLike

    object ServiceError {
      case object NotFound extends ServiceError {
        override def asApiError: ApiError = ApiError.NotFound()
      }
    }

  }

}
