package gp.users

import cats.effect.{IO, Ref}
import gp.users.model.User

trait UsersService[F[_]] {

  def get(id: String): F[Option[User]]
  def getByLogin(login: String): F[Option[User]]

  def put(id: String, user: User): F[User]

}

object UsersService {

  class InMemory extends UsersService[IO] {
    import com.github.t3hnar.bcrypt._
    private val usersRef = Ref.ofEffect(IO.pure(Map[String, User]("1" -> User("1", "admin", "admin".boundedBcrypt))))

    override def get(id: String): IO[Option[User]] = usersRef.flatMap(ref => ref.get.map(_.get(id)))
    override def getByLogin(login: String): IO[Option[User]] =
      usersRef.flatMap(_.get.map(_.values.find(_.name == login)))

    override def put(id: String, user: User): IO[User] = usersRef.map(_.update(users => users + (id -> user))).as(user)

  }

}
