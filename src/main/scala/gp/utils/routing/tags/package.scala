package gp.utils.routing

import sttp.tapir.{endpoint, EndpointInput}

package object tags {

  trait RouteTag {
    def name: String

    def input: EndpointInput[Unit] = endpoint.input / name
    def tags: List[String] = List(name)
  }

  object RouteTag {

    case object Auth extends RouteTag {
      override def name: String = "auth"
    }

  }

}