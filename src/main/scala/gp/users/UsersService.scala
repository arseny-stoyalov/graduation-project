package gp.users

import cats.effect.{IO, Ref}
import gp.users.model.User

import java.util.UUID

trait UsersService[F[_]] {

  def get(id: UUID): F[Option[User]]
  def getByLogin(login: String): F[Option[User]]

  def put(id: UUID, user: User): F[User]

}

object UsersService {

  class InMemory extends UsersService[IO] {
    import com.github.t3hnar.bcrypt._

    private lazy val adminId = UUID.fromString("cbc07061-d6bf-4c25-af6d-0051ab533878")
    private val usersRef = Ref.ofEffect(IO.pure(Map[String, User](adminId.toString -> User(adminId, "admin", "admin".boundedBcrypt))))

    override def get(id: UUID): IO[Option[User]] = usersRef.flatMap(ref => ref.get.map(_.get(id.toString)))
    override def getByLogin(login: String): IO[Option[User]] =
      usersRef.flatMap(_.get.map(_.values.find(_.name == login)))

    override def put(id: UUID, user: User): IO[User] = usersRef.map(_.update(users => users + (id.toString -> user))).as(user)

  }

}
