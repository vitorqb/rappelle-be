package auth

import scala.concurrent.Future
import play.api.db.Database
import anorm.SQL
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import services.UniqueIdGeneratorLike
import anorm.JodaParameterMetaData._
import java.sql.Connection
import play.api.Logger

/** Stores information about email confirmations.
  */
trait EmailConfirmationRepositoryLike {
  def create(request: CreateEmailConfirmationRequest): Future[EmailConfirmation]
  def read(key: String): Future[Option[EmailConfirmation]]
  def read(id: Int): Future[Option[EmailConfirmation]]
  def update(
      request: UpdateEmailConfirmationRequest
  ): Future[Option[EmailConfirmation]]
}

/** Main implementation for email confirmation repository.
  */
class EmailConfirmationRepository(
    db: Database,
    idGenerator: UniqueIdGeneratorLike
)(implicit
    val ec: ExecutionContext
) extends EmailConfirmationRepositoryLike {

  import EmailConfirmationSqlParsers._

  val table          = "emailConfirmations"
  private val logger = Logger(getClass())

  def create(
      request: CreateEmailConfirmationRequest
  ): Future[EmailConfirmation] = {
    logger.info(f"Creation for userId ${request.userId}")
    Future {
      db.withConnection { implicit c =>
        SQL(
          f"""INSERT INTO ${table}(id, userId, key, sentAt, responseReceivedAt)
              VALUES ({id}, {userId}, {key}, {sentAt}, {responseReceivedAt})"""
        )
          .on(
            "id"                 -> idGenerator.gen(),
            "userId"             -> request.userId,
            "key"                -> request.key,
            "sentAt"             -> request.sentAt,
            "responseReceivedAt" -> Option.empty[DateTime]
          )
          .execute()
      }
    } flatMap { _ =>
      logger.info(f"Finished creation for userId ${request.userId}")
      read(request.key).map(_.get)
    }
  }

  override def update(
      request: UpdateEmailConfirmationRequest
  ): Future[Option[EmailConfirmation]] = {
    implicit val connection = db.getConnection(false)
    connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
    Future {
      read(request.id).flatMap {
        case Some(emailConfirmation) => {
          SQL(
            f"""UPDATE ${table}
                  SET responseReceivedAt={responseReceivedAt}
                  WHERE id={id}"""
          ).on(
            "id" -> request.id,
            "responseReceivedAt" -> request.responseReceivedAt.getOrElse(
              emailConfirmation.responseReceivedAt
            )
          ).execute()
          connection.commit()
          read(request.id)
        }
        case None => Future.successful(None)
      }
    }.flatten.andThen { case _ =>
      connection.commit()
      connection.close()
    }
  }

  override def read(key: String): Future[Option[EmailConfirmation]] = Future {
    logger.info(f"Reading...")
    db.withConnection { implicit c =>
      SQL(f"SELECT * FROM ${table} WHERE key={key}")
        .on("key" -> key)
        .as(emailConfirmationParser.*)
        .headOption
    }
  }

  override def read(id: Int): Future[Option[EmailConfirmation]] = Future {
    logger.info(f"Reading...")
    db.withConnection { implicit c =>
      SQL(f"SELECT * FROM ${table} WHERE id={id}")
        .on("id" -> id)
        .as(emailConfirmationParser.*)
        .headOption
    }
  }

}

/** Fake implementation for local dev/testing
  */
class FakeEmailConfirmationRepository extends EmailConfirmationRepositoryLike {

  override def create(
      request: CreateEmailConfirmationRequest
  ): Future[EmailConfirmation] = ???

  override def read(key: String): Future[Option[EmailConfirmation]] = ???

  override def read(id: Int): Future[Option[EmailConfirmation]] = ???

  override def update(
      request: UpdateEmailConfirmationRequest
  ): Future[Option[EmailConfirmation]] = ???

}

/** Sql Parsers
  */
object EmailConfirmationSqlParsers {

  import anorm.SqlParser._
  import anorm._

  val emailConfirmationParser = {
    (get[Int]("id") ~ get[Int]("userId") ~ get[String]("key") ~ get[DateTime](
      "sentAt"
    ) ~
      get[Option[DateTime]]("responseReceivedAt")) map {
      case id ~ userId ~ key ~ sentAt ~ responseReceivedAt =>
        EmailConfirmation(id, userId, key, sentAt, responseReceivedAt)
    }
  }

}
