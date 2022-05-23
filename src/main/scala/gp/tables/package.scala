package gp

import gp.utils.routing.errors.ApiErrorLike

package object tables {

  object errors {

    trait TableError extends ApiErrorLike

  }

}
