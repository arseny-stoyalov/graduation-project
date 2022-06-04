package gp.auth

import com.github.t3hnar.bcrypt._
import cats.Monad
import cats.data.EitherT
import gp.users.model.User
import cats.syntax.either._
import cats.syntax.functor._
import gp.auth.AuthService.JWT
import gp.auth.model.AuthModel.UserToken
import gp.config.JWTConfig
import gp.users.UsersService
import io.circe.jawn
import io.circe.syntax._
import pdi.jwt.exceptions.JwtExpirationException
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.time.Instant
import java.util.UUID

class AuthService[F[_]: Monad](config: JWTConfig, usersService: UsersService[F]) {

  private val jwtAlg = JwtAlgorithm.HS256

  def createToken(login: String, password: String): EitherT[F, AuthenticationError, String] =
    EitherT {
      for {
        user <- usersService.getByLogin(login)
      } yield user
        .toRight(AuthenticationError.InvalidLoginOrPass)
        .flatMap(user =>
          Either.cond(password.isBcryptedBounded(user.password), toJWT(user), AuthenticationError.InvalidLoginOrPass)
        )
    }

  def getUserFromToken(token: JWT): EitherT[F, AuthenticationError, User] = {
    val userId: Either[AuthenticationError, UUID] = for {
      claim <- JwtCirce
        .decode(token, config.secretKey, Seq(jwtAlg))
        .toEither
        .leftMap {
          case _: JwtExpirationException => AuthenticationError.ExpiredToken
          case _ => AuthenticationError.InvalidToken
        }
      ut <- jawn.decode[UserToken](claim.content).leftMap(_ => AuthenticationError.InvalidToken)
    } yield ut.userId

    EitherT
      .fromEither(userId)
      .semiflatMap(id => usersService.get(id))
      .subflatMap(_.toRight(AuthenticationError.InvalidToken))
  }

  private def toJWT(user: User): String =
    JwtCirce.encode(
      JwtClaim(
        content = UserToken(user.id).asJson.noSpaces,
        expiration = Some(Instant.now.plusSeconds(config.defaultExpire.toSeconds).getEpochSecond),
        issuedAt = Some(Instant.now.getEpochSecond)
      ),
      config.secretKey,
      jwtAlg
    )

}

object AuthService {

  type JWT = String

}
