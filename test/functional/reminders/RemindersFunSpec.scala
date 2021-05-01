package reminders

import functional.utils.WithAuthContext
import functional.utils.AuthContext
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import functional.utils.FunctionalSpec
import common.PaginationOptions

class RemindersFunSpec extends FunctionalSpec with ScalaFutures {

  "create reminder flow" should {
    "create and get a reminder for a given user" in {
      WithTestContext() { c =>
        //First, get empty reminders
        val remindersBefore = c.authContext
          .requestWithToken(
            routes.RemindersController.listReminders(c.paginationOpts).toString()
          )
          .get()
          .futureValue
        remindersBefore.status must equal(200)
        remindersBefore.json must equal(
          Json.obj("items" -> Json.arr(), "totalCount" -> 0, "page" -> 1)
        )

        //Then, create a reminder
        val reminder = c.authContext
          .requestWithToken(
            routes.RemindersController.postReminder().toString()
          )
          .post(
            Json.obj("title" -> "Reminder", "datetime" -> "2020-01-01T00:00:00")
          )
          .futureValue
        reminder.status must equal(200)
        (reminder.json \ "title").as[String] must equal("Reminder")
        (reminder.json \ "id").as[Int] must equal(1)

        //Query for reminders
        val remindersAfter = c.authContext
          .requestWithToken(
            routes.RemindersController.listReminders(c.paginationOpts).toString()
          )
          .get()
          .futureValue
        remindersAfter.status must equal(200)
        remindersAfter.json must equal(
          Json.obj(
            "items"      -> Json.arr(reminder.json),
            "page"       -> 1,
            "totalCount" -> 1
          )
        )
      }
    }
  }

  case class TestContext(
      authContext: AuthContext,
      paginationOpts: PaginationOptions = PaginationOptions(1, 10)
  )

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      WithAuthContext() { authContext =>
        block(TestContext(authContext))
      }
    }
  }

}
