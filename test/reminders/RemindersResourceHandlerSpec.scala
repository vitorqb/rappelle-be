package reminders

import org.scalatestplus.play.PlaySpec
import org.mockito.IdiomaticMockito
import scala.concurrent.Future
import testutils.Fixtures
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext

class RemindersResourceHandlerSpec
    extends PlaySpec
    with IdiomaticMockito
    with ScalaFutures {

  implicit val ec = ExecutionContext.global

  "listReminders" should {
    "request the reminders from the repository" in {
      WithTestContext() { c =>
        c.repo.list(c.listReq) shouldReturn Future.successful(
          Seq(Fixtures.aReminder)
        )
        val result = c.handler.listReminders(c.listReq).futureValue
        result must equal(ListReminderResponse(Seq(Fixtures.aReminder)))
      }
    }
  }

  "createReminder" should {
    "request creation from the repository" in {
      WithTestContext() { c =>
        c.repo.create(c.createReq) shouldReturn Future.successful(
          Fixtures.aReminder
        )
        val result = c.handler.createReminder(c.createReq).futureValue
        result must equal(Fixtures.aReminder)
      }
    }
  }

  case class TestContext(
      listReq: ListReminderRequest,
      createReq: CreateReminderRequest,
      handler: RemindersResourceHandler,
      repo: RemindersRepositoryLike
  )

  object WithTestContext {

    def apply()(block: TestContext => Any): Any = {
      val repo = mock[RemindersRepositoryLike]
      block(
        TestContext(
          listReq = ListReminderRequest(Fixtures.anUser),
          createReq = CreateReminderRequest(
            Fixtures.anUser,
            Fixtures.aReminder.title,
            Fixtures.aReminder.datetime
          ),
          handler = new RemindersResourceHandler(repo),
          repo = repo
        )
      )
    }

  }

}
