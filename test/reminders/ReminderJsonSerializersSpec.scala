package reminders

import testutils.Fixtures
import reminders.ReminderJsonSerializers._
import play.api.libs.json.Json
import org.scalatestplus.play.PlaySpec

class ReminderJsonSerializersSpec extends PlaySpec {

  "listReminderResponseWrites" should {
    "serialize a response" in {
      val reminder1 = Fixtures.aReminder
      val reminder2 = Fixtures.aReminder.copy(id = 2)
      val response = ListReminderResponse(Seq(reminder1, reminder2))
      Json.toJson(response) must equal(
        Json.obj(
          "items" -> Seq(Json.toJson(reminder1), Json.toJson(reminder2))
        )
      )

    }
  }

  "reminderWrites" should {
    "serialize a reminder" in {
      Json.toJson(Fixtures.aReminder) must equal(
        Json.obj(
          "id" -> Fixtures.aReminder.id,
          "title" -> Fixtures.aReminder.title,
          "datetime" -> "2021-11-14T22:11:13.000+01:00"
        )
      )
    }
  }
}
