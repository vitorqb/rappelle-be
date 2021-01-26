package testutils

import reminders.Reminder
import org.joda.time.DateTime
import auth.User

object Fixtures {

  def aReminder =
    Reminder("1", "Reminder", DateTime.parse("2021-11-14T22:11:13"))

  def anUser = User(1, "user@test.com", true)

}
