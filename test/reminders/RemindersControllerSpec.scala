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

  case class TestContext(
      handler: RemindersResourceHandlerLike,
      controller: RemindersController,
      listRequest: ListReminderRequest
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
          listRequest = ListReminderRequest(Fixtures.anUser)
        )
      )
    }
  }

}
