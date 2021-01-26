package reminders

import org.joda.time.DateTime
import auth.User

// Reminders
//!!!! TODO id to string
case class Reminder(id: String, title: String, datetime: DateTime)
case class ListReminderRequest(user: User)
case class ListReminderResponse(items: Seq[Reminder])
