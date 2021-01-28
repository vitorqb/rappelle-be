package reminders

import org.joda.time.DateTime
import auth.User

// Reminders
case class Reminder(id: Int, title: String, datetime: DateTime)
case class ListReminderRequest(user: User)
case class ListReminderResponse(items: Seq[Reminder])
case class CreateReminderRequest(user: User, title: String, datetime: DateTime)
case class CreateReminderRequestInput(title: String, datetime: DateTime)
