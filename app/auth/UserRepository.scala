package auth

import scala.concurrent.Future
import play.api.db.Database
import anorm.SQL
import services.UniqueIdGeneratorLike
import scala.concurrent.ExecutionContext
import services.PasswordHashSvcLike
import anorm.SqlParser
import play.api.Logger

/** A trait responsible for persisting users.
  */
trait UserRepositoryLike {
  def create(request: CreateUserRequest): Future[User]
  def read(email: String): Future[Option[User]]
  def read(id: Int): Future[Option[User]]
  def update(request: UpdateUserRequest): Future[Option[User]]
  def passwordIsValid(user: User, password: String): Future[Boolean]
}

class UserRepository(
    db: Database,
    idGenerator: UniqueIdGeneratorLike,
    hashSvc: PasswordHashSvcLike
)(implicit ec: ExecutionContext)
    extends UserRepositoryLike {

  private val table = "users"

  private val logger = Logger(getClass())

  override def create(request: CreateUserRequest): Future[User] = Future {
    val id = idGenerator.gen()
    logger.info(f"Handling create with id $id and email ${request.email}")
    db.withConnection { implicit c =>
      SQL(
        s"""INSERT INTO ${table} (id, email, passwordHash, emailConfirmed) 
            VALUES ({id}, {email}, {passwordHash}, false)"""
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
    logger.info(f"Handling read with email $email")
    db.withConnection { implicit c =>
      SQL(s"SELECT * FROM ${table} WHERE email={email}")
        .on("email" -> email)
        .as(UserSqlParsers.userParser.*)
        .headOption
    }
  }

  def read(id: Int): Future[Option[User]] = Future {
    logger.info(f"Handling read with id $id")
    db.withConnection { implicit c =>
      SQL(s"SELECT * FROM ${table} WHERE id={id}")
        .on("id" -> id)
        .as(UserSqlParsers.userParser.*)
        .headOption
    }
  }

  override def update(request: UpdateUserRequest): Future[Option[User]] = {
    logger.info(f"Handling update for user with id ${request.userId}")
    implicit val connection = db.getConnection(false)
    read(request.userId).flatMap {
      case None => Future.successful(None)
      case Some(user) => {
        SQL(
          s"""UPDATE ${table}
                SET emailConfirmed={emailConfirmed}
                WHERE id={id}"""
        )
          .on(
            "emailConfirmed" -> request.emailConfirmed
              .getOrElse(user.emailConfirmed),
            "id" -> request.userId
          )
          .execute()
        connection.commit()
        read(request.userId)
      }
    } andThen { case _ =>
      connection.close()
    }
  }

  override def passwordIsValid(
      user: User,
      password: String
  ): Future[Boolean] = {
    logger.info(f"Checking password for user $user")
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
}

/** A fake implementation of an user repository that keep an in-memory map of users.
  */
class FakeUserRepository extends UserRepositoryLike {

  implicit val ec = ExecutionContext.global

  var users: Seq[(User, String)] = Seq()

  def create(request: CreateUserRequest, id: Int, emailConfirmed: Boolean) = {
    val user = User(id, request.email, emailConfirmed)
    users ++= Seq((user, request.password))
    Future.successful(user)

  }

  def create(request: CreateUserRequest): Future[User] = {
    val id = users.map(_._1).map(_.id).maxOption.getOrElse(0) + 1
    create(request, id, false)
  }

  override def read(email: String): Future[Option[User]] =
    Future.successful(users.map(_._1).find(_.email == email))

  override def read(id: Int): Future[Option[User]] =
    Future.successful(users.map(_._1).find(_.id == id))

  override def update(request: UpdateUserRequest): Future[Option[User]] =
    read(request.userId).flatMap {
      case Some(user) => {
        val index = users.indexWhere { case (u, _) => u == user }
        val newUser = user.copy(emailConfirmed =
          request.emailConfirmed.getOrElse(user.emailConfirmed)
        )
        users.updated(index, newUser)
        read(request.userId)
      }
      case None => Future.successful(None)
    }

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
    (get[Int]("id") ~ get[String]("email") ~ get[Boolean]("emailConfirmed"))
      .map { case id ~ email ~ emailConfirmed =>
        User(id, email, emailConfirmed)
      }
  }
}
