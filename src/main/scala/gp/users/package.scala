package gp

import gp.utils.routing.errors.ApiErrorLike

package object users {

  object errors {

    sealed trait UserError extends ApiErrorLike

  }

}
