package gp.config

import scala.concurrent.duration.Duration

case class JWTConfig(secretKey: String, defaultExpire: Duration)
