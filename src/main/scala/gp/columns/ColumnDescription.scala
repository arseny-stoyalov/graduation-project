package gp.columns

import cats.syntax.either._
import skunk.Codec
import skunk.data.Type

case class ColumnDescription(name: String) {

  def w = Codec.simple(identity[String], _.asRight[String], Type("string"))
}
