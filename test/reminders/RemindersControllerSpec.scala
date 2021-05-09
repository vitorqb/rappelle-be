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
import play.api.libs.json.JodaWrites._
import common.PaginationOptions

class RemindersControllerSpec extends PlaySpec with IdiomaticMockito {

  implicit val ec = ExecutionContext.global

  "listReminders" should {

    "query the handler for a list of reminders" in {
      WithTestContext() { c =>
        c.handler.listReminders(c.listRequest) shouldReturn Future.successful(
          c.listRemindersResp
        )
        val response = c.controller.listReminders(c.paginationOpts)(FakeRequest())
        Helpers.status(response) must equal(200)
        Helpers.contentAsJson(response) must equal(
          Json.toJson(c.listRemindersResp)
        )
      }
    }

    "passes pagination options to handler" in {
      WithTestContext() { c =>
        val listReq        = c.listRequest.copy(itemsPerPage = 100, page = 9)
        val paginationOpts = c.paginationOpts.copy(itemsPerPage = 100, page = 9)
        c.handler.listReminders(listReq) shouldReturn Future.successful(c.listRemindersResp)
        Helpers.contentAsJson(c.controller.listReminders(paginationOpts)(FakeRequest())) must equal(
          Json.toJson(c.listRemindersResp)
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
            "title"    -> Fixtures.aReminder.title,
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

  "deletereminder" should {

    "returns 200 when reminder delete" in {
      WithTestContext() { c =>
        val deleteReq = DeleteReminderRequest(id = 1, user = Fixtures.anUser)
        c.handler.deleteReminder(deleteReq) shouldReturn Future.successful(())

        val result = c.controller.deleteReminder(1)(FakeRequest())

        Helpers.status(result) must equal(200)
      }
    }

    "returns 404 when reminder not found for user" in {
      WithTestContext() { c =>
        val deleteReq = DeleteReminderRequest(id = 1, user = Fixtures.anUser)
        c.handler.deleteReminder(deleteReq) shouldReturn Future.failed(new ReminderNotFound())

        val result = c.controller.deleteReminder(1)(FakeRequest())

        Helpers.status(result) must equal(404)
      }
    }
  }

  case class TestContext(
      handler: RemindersResourceHandlerLike,
      controller: RemindersController,
      paginationOpts: PaginationOptions = PaginationOptions(1, 2),
      listRequest: ListReminderRequest = ListReminderRequest(Fixtures.anUser, 2, 1),
      listRemindersResp: ListReminderResponse = ListReminderResponse(Seq(Fixtures.aReminder)),
      createRequest: CreateReminderRequest = CreateReminderRequest(
        Fixtures.anUser,
        Fixtures.aReminder.title,
        Fixtures.aReminder.datetime
      )
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
          )
        )
      )
    }
  }

}
