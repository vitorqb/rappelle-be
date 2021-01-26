package reminders

import scala.concurrent.Future

trait RemindersRepositoryLike {
  def list(req: ListReminderRequest): Future[Seq[Reminder]]
}

class RemindersRepository extends RemindersRepositoryLike {

  override def list(req: ListReminderRequest): Future[Seq[Reminder]] = ???

}
