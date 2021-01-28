package reminders

import testutils.Fixtures
import reminders.ReminderJsonSerializers._
import play.api.libs.json.Json
import org.scalatestplus.play.PlaySpec
import org.joda.time.format.ISODateTimeFormat

class ReminderJsonSerializersSpec extends PlaySpec {

  "listReminderResponseWrites" should {
    "serialize a response" in {
      val reminder1 = Fixtures.aReminder
      val reminder2 = Fixtures.aReminder.copy(id = 2)
      val response = ListReminderResponse(Seq(reminder1, reminder2), 20)
      Json.toJson(response) must equal(
        Json.obj(
          "items" -> Seq(Json.toJson(reminder1), Json.toJson(reminder2)),
          "totalCount" -> 20
        )
      )

    }
  }

  "reminderWrites" should {
    "serialize a reminder" in {
      val expDateTime =
        ISODateTimeFormat.dateTime().print(Fixtures.aReminder.datetime)
      Json.toJson(Fixtures.aReminder) must equal(
        Json.obj(
          "id" -> Fixtures.aReminder.id,
          "title" -> Fixtures.aReminder.title,
          "datetime" -> expDateTime
        )
      )
    }
  }
}
