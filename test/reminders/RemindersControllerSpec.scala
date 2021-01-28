package reminders

import org.scalatestplus.play.PlaySpec
import testutils.Fixtures
import scala.concurrent.Future
import play.api.test.Helpers
import play.api.libs.json.Json
import org.mockito.IdiomaticMockito
import play.api.test.FakeRequest
import play.api.test.Helpers.defaultAwaitTimeout
import auth.FakeRequestUserExtractor
import scala.concurrent.ExecutionContext
import ReminderJsonSerializers._
import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._

class RemindersControllerSpec extends PlaySpec with IdiomaticMockito {

  implicit val ec = ExecutionContext.global

  "listReminders" should {

    "query the handler for a list of reminders" in {
      WithTestContext() { c =>
        val listRemindersResp = ListReminderResponse(Seq(Fixtures.aReminder))
        c.handler.listReminders(c.listRequest) shouldReturn Future.successful(
          listRemindersResp
        )
        val response = c.controller.listReminders(FakeRequest())
        Helpers.status(response) must equal(200)
        Helpers.contentAsJson(response) must equal(
          Json.toJson(listRemindersResp)
        )
      }
    }

  }

  "postReminder" should {

    "ask the handler to create the reminder" in {
      WithTestContext() { c =>
        c.handler.createReminder(c.createRequest) shouldReturn Future
          .successful(
            Fixtures.aReminder
          )
        val request = FakeRequest().withBody(
          Json.obj(
            "title" -> Fixtures.aReminder.title,
            "datetime" -> Fixtures.aReminder.datetime
          )
        )
        val response = c.controller.postReminder()(request)
        Helpers.status(response) must equal(200)
        Helpers.contentAsJson(response) must equal(
          Json.toJson(Fixtures.aReminder)
        )
      }
    }

  }

  case class TestContext(
      handler: RemindersResourceHandlerLike,
      controller: RemindersController,
      listRequest: ListReminderRequest,
      createRequest: CreateReminderRequest
  )
  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      val handler = mock[RemindersResourceHandlerLike]
      block(
        TestContext(
          handler,
          controller = new RemindersController(
            Helpers.stubControllerComponents(),
            handler,
            new FakeRequestUserExtractor(Some(Fixtures.anUser))
          ),
          listRequest = ListReminderRequest(Fixtures.anUser),
          createRequest = CreateReminderRequest(
            Fixtures.anUser,
            Fixtures.aReminder.title,
            Fixtures.aReminder.datetime
          )
        )
      )
    }
  }

}
