package gp

import gp.utils.routing.errors.{ApiError, ApiErrorLike}

package object auth {

  sealed trait AuthenticationError extends ApiErrorLike

  object AuthenticationError {
    case object InvalidToken extends AuthenticationError {
      override def asApiError: ApiError = ApiError.Unauthorized("invalid token")
    }

    case object ExpiredToken extends AuthenticationError {
      override def asApiError: ApiError = ApiError.Unauthorized("expired token")
    }

    case object InvalidLoginOrPass extends AuthenticationError {
      override def asApiError: ApiError = ApiError.Unauthorized("invalid login or pass")
    }
  }

}
