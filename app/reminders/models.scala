package reminders

import org.joda.time.DateTime
import auth.User

// Reminders
case class Reminder(id: Int, title: String, datetime: DateTime)
//!!!! TODO REMOVE DEFAULT
case class ListReminderRequest(user: User, itemsPerPage: Int = 10, page: Int = 1)
//!!!! TODO REMOVE DEFAULT
case class ListReminderResponse(items: Seq[Reminder], totalCount: Int = 11)
case class CreateReminderRequest(user: User, title: String, datetime: DateTime)
case class CreateReminderRequestInput(title: String, datetime: DateTime)
