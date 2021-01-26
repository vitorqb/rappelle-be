package reminders

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import scala.concurrent.Future

@ImplementedBy(classOf[RemindersResourceHandler])
trait RemindersResourceHandlerLike {

  def listReminders(req: ListReminderRequest): Future[ListReminderResponse] = {
    ???
  }

}

class RemindersResourceHandler @Inject() () extends RemindersResourceHandlerLike
