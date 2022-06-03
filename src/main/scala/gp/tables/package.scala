package gp

import gp.utils.routing.errors.{ApiError, ApiErrorLike}

package object tables {

  object errors {

    sealed trait TableError extends ApiErrorLike

    object TableError {
      case object NotFound extends TableError {
        override def asApiError: ApiError = ApiError.NotFound()
      }
    }

  }

}
