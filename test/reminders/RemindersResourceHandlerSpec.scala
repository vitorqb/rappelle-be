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

  case class TestContext(
      listReq: ListReminderRequest,
      handler: RemindersResourceHandler,
      repo: RemindersRepositoryLike
  )

  object WithTestContext {

    def apply()(block: TestContext => Any): Any = {
      val repo = mock[RemindersRepositoryLike]
      block(
        TestContext(
          listReq = ListReminderRequest(Fixtures.anUser),
          handler = new RemindersResourceHandler(repo),
          repo = repo
        )
      )
    }

  }

}
