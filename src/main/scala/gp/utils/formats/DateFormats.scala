package gp.utils.formats

import io.circe.{Decoder, Encoder}
import cats.syntax.either._

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object DateFormats {

  private def format1 = new SimpleDateFormat("dd.MM.yyyy")
  private def format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  private def format3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  private def format4 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")

  implicit val encoder: Encoder[Date] = Encoder.encodeString.contramap { date =>
    canonicalFormat.format(date)
  }

  implicit val decoder: Decoder[Date] = Decoder.decodeString.emap {
    case x if x.isEmpty || x == "none" =>
      Left("failed to parse date from empty string")
    case x if x.length == 19 =>
      Either.catchNonFatal(format2.parse(x)).leftMap(_.getMessage)
    case x if x.length == 10 =>
      Either.catchNonFatal(format1.parse(x)).leftMap(_.getMessage)
    case x if x.length == 20 && x.endsWith("Z") =>
      Either.catchNonFatal(format3.parse(x.substring(0, x.length - 1))).leftMap(_.getMessage)
    case x if x.endsWith("Z") =>
      Either.catchNonFatal(format4.parse(x)).leftMap(_.getMessage)
    case x =>
      Either.catchNonFatal(canonicalFormat.parse(x)).leftMap(_.getMessage)
  }

  def canonicalFormat = {
    val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    f.setTimeZone(TimeZone.getTimeZone("UTC"))
    f
  }

}
