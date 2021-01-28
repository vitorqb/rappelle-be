package reminders

import scala.concurrent.Future
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import play.api.db.Database
import scala.concurrent.ExecutionContext
import services.UniqueIdGeneratorLike
import play.api.Logger
import anorm.SQL
import anorm.JodaParameterMetaData._
import org.joda.time.DateTime

@ImplementedBy(classOf[RemindersRepository])
trait RemindersRepositoryLike {
  def create(req: CreateReminderRequest): Future[Reminder]
  def read(id: Int): Future[Option[Reminder]]
  def list(req: ListReminderRequest): Future[Seq[Reminder]]
}

class RemindersRepository @Inject() (
    db: Database,
    idGenerator: UniqueIdGeneratorLike
)(implicit
    ec: ExecutionContext
) extends RemindersRepositoryLike {

  private val table = "reminders"

  private val logger = Logger(getClass())

  override def read(id: Int): Future[Option[Reminder]] = {
    logger.info(f"Recovering rmeinder with id $id")
    Future {
      db.withConnection { implicit c =>
        SQL(f"""SELECT * FROM ${table} WHERE id = {id}""")
          .on("id" -> id)
          .as(RemindersSqlParsers.reminderParser.*)
          .headOption
      }
    }
  }

  override def create(req: CreateReminderRequest): Future[Reminder] = {
    val id = idGenerator.gen()
    logger.info(
      f"Creating new reminder with id $id from request with title" +
        " ${req.title} for user ${req.user.email}"
    )
    Future {
      db.withConnection { implicit c =>
        SQL(f"""INSERT INTO ${table} (id, userId, title, datetime)
                VALUES ({id}, {userId}, {title}, {datetime})""")
          .on(
            "id" -> id,
            "userId" -> req.user.id,
            "title" -> req.title,
            "datetime" -> req.datetime
          )
          .execute()
        read(id).map(_.get)
      }
    }.flatten
  }

  override def list(req: ListReminderRequest): Future[Seq[Reminder]] = {
    logger.info(f"Recovering reminder for req $req")
    Future {
      db.withConnection { implicit c =>
        SQL(f"""SELECT * FROM ${table} WHERE userId = {userId}""")
          .on("userId" -> req.user.id)
          .as(RemindersSqlParsers.reminderParser.*)
      }
    }
  }

}

object RemindersSqlParsers {

  import anorm.SqlParser._
  import anorm._

  val reminderParser: RowParser[Reminder] = {
    (get[Int]("id") ~ get[String]("title") ~ get[DateTime]("datetime"))
      .map { case id ~ title ~ datetime =>
        Reminder(id, title, datetime)
      }
  }

}
