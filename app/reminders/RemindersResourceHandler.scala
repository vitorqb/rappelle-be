package reminders

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[RemindersResourceHandler])
trait RemindersResourceHandlerLike {

  def listReminders(req: ListReminderRequest): Future[ListReminderResponse]
  def createReminder(req: CreateReminderRequest): Future[Reminder]

}

class RemindersResourceHandler @Inject() (repo: RemindersRepositoryLike)(
    implicit val ec: ExecutionContext
) extends RemindersResourceHandlerLike {

  override def listReminders(
      req: ListReminderRequest
  ): Future[ListReminderResponse] = {
    repo.list(req).map(x => ListReminderResponse(x))
  }

  override def createReminder(req: CreateReminderRequest): Future[Reminder] =
    ???

}
