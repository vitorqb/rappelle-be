package auth

import scala.concurrent.Future
import play.api.db.Database
import anorm.SQL
import services.UniqueIdGeneratorLike
import scala.concurrent.ExecutionContext
import services.PasswordHashSvcLike
import anorm.SqlParser

/** A trait responsible for persisting users.
  */
trait UserRepositoryLike {
  def create(request: CreateUserRequest): Future[User]
  def read(email: String): Future[Option[User]]
  def read(id: Int): Future[Option[User]]
  def passwordIsValid(user: User, password: String): Future[Boolean]
}

class UserRepository(
    db: Database,
    idGenerator: UniqueIdGeneratorLike,
    hashSvc: PasswordHashSvcLike
)(implicit ec: ExecutionContext)
    extends UserRepositoryLike {

  private val table = "users"

  override def create(request: CreateUserRequest): Future[User] = Future {
    val id = idGenerator.gen()
    db.withConnection { implicit c =>
      SQL(
        s"INSERT INTO ${table} (id, email, passwordHash) VALUES ({id}, {email}, {passwordHash})"
      )
        .on(
          "id" -> id,
          "email" -> request.email,
          "passwordHash" -> hashSvc.hash(request.password)
        )
        .execute()
      read(request.email).map(_.get)
    }
  }.flatten

  override def read(email: String): Future[Option[User]] = Future {
    db.withConnection { implicit c =>
      SQL(s"SELECT * FROM ${table} WHERE email={email}")
        .on("email" -> email)
        .as(UserSqlParsers.userParser.*)
        .headOption
    }
  }

  def read(id: Int): Future[Option[User]] = ???

  override def passwordIsValid(user: User, password: String): Future[Boolean] =
    Future {
      db.withConnection { implicit c =>
        SQL(s"SELECT passwordHash FROM ${table} WHERE id={id}")
          .on("id" -> user.id)
          .as(SqlParser.str("passwordHash").*)
          .headOption match {
          case None               => false
          case Some(passwordHash) => hashSvc.hash(password) == passwordHash
        }
      }
    }
}

/** A fake implementation of an user repository that keep an in-memory map of users.
  */
class FakeUserRepository extends UserRepositoryLike {

  var users: Seq[(User, String)] = Seq()

  override def create(request: CreateUserRequest): Future[User] =
    create(request, users.map(_._1.id).max + 1)

  def create(request: CreateUserRequest, id: Int): Future[User] = {
    val user = User(id, request.email)
    users ++= Seq((user, request.password))
    Future.successful(user)
  }

  override def read(email: String): Future[Option[User]] =
    Future.successful(users.map(_._1).find(_.email == email))

  override def read(id: Int): Future[Option[User]] =
    Future.successful(users.map(_._1).find(_.id == id))

  override def passwordIsValid(user: User, password: String): Future[Boolean] =
    Future.successful {
      users
        .find(_._1.email == user.email)
        .filter(_._2 == password)
        .map(_._1)
        .isDefined
    }

}

object UserSqlParsers {

  import anorm.SqlParser._
  import anorm._

  val userParser: RowParser[User] = {
    (get[Int]("id") ~ get[String]("email")) map { case id ~ email =>
      User(id, email)
    }
  }
}
