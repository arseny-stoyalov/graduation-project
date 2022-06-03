package gp.utils.routing

import io.circe.generic.JsonCodec

package object errors {

  trait ApiErrorLike {
    def asApiError: ApiError
  }

  sealed trait ApiError

  object ApiError {

    @JsonCodec
    case class Unauthorized(msg: String) extends ApiError

    @JsonCodec
    case class UnprocessableEntity(msg: String) extends ApiError

    @JsonCodec
    case class NotFound() extends ApiError
  }

}
