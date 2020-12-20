package auth

import scala.concurrent.Future
import play.api.db.Database
import anorm.SQL
import anorm.JodaParameterMetaData._
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import services.UniqueIdGeneratorLike

/** Trait for the repository of authentication tokens.
  */
trait AuthTokenRepositoryLike {

  /** Creates a new token for a given CreateTokenRequest.
    */
  def create(request: CreateTokenRequest): Future[Token]

  /** Retrieves a token by id.
    */
  def read(tokenValue: String): Future[Option[Token]]

}

/** The main implementation for the repository of authentication tokens.
  */
class AuthTokenRepository(
    db: Database,
    uniqueIdGenerator: UniqueIdGeneratorLike
)(implicit val ec: ExecutionContext)
    extends AuthTokenRepositoryLike {

  final protected val table = "authTokens"

  def create(request: CreateTokenRequest): Future[Token] = {
    Future {
      db.withConnection { implicit t =>
        SQL(
          s"""INSERT INTO ${table} (id, userId, expiresAt, tokenValue)
              VALUES ({id}, {userId}, {expiresAt}, {tokenValue})"""
        )
          .on(
            "id" -> uniqueIdGenerator.gen(),
            "userId" -> request.user.id,
            "expiresAt" -> request.expiresAt,
            "tokenValue" -> request.value
          )
          .execute()
      }
      read(request.value).map(_.get)
    }.flatten
  }

  def read(tokenValue: String): Future[Option[Token]] = {
    Future {
      db.withConnection { implicit c =>
        SQL(s"SELECT * FROM ${table} WHERE tokenValue={tokenValue}")
          .on(
            "tokenValue" -> tokenValue
          )
          .as(AuthSqlParsers.tokenParser.*)
          .headOption
      }
    }
  }

}

/** A dummy (test) implementation for the repository of authentication tokens.
  * Simply returns always the same token and validates always the same user.
  */
class DummyAuthTokenRepository(user: User, password: String, token: Token)
    extends AuthTokenRepositoryLike {

  def create(request: CreateTokenRequest): Future[Token] =
    Future.successful(token)

  def read(tokenValue: String) =
    if (tokenValue == token.value)
      Future.successful(Some(token))
    else
      Future.successful(None)

}

/** The sql parsers
  */
object AuthSqlParsers {

  import anorm.SqlParser._
  import anorm._

  val tokenParser: RowParser[Token] = {
    (get[DateTime]("expiresAt") ~ get[String]("tokenValue") ~ get[Int](
      "userId"
    )) map { case dateTime ~ tokenValue ~ userId =>
      Token(tokenValue, dateTime, userId)
    }
  }

}
