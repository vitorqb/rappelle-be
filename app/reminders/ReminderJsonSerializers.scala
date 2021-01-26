package reminders

import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime

object ReminderJsonSerializers {

  implicit val reminderWrites: Writes[Reminder] =
    (
      (JsPath \ "id").write[String] and
        (JsPath \ "title").write[String] and
        (JsPath \ "datetime").write[DateTime]
    )(unlift(Reminder.unapply))

  implicit val listReminderResponseWrites: Writes[ListReminderResponse] =
    (
      (JsPath \ "items").write[Seq[Reminder]] and (JsPath \ "items")
        .write[Seq[Reminder]]
    )(x => (x.items, x.items))

}
