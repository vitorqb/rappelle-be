package reminders

import org.scalatestplus.play.PlaySpec
import org.mockito.IdiomaticMockito
import scala.concurrent.Future
import testutils.Fixtures
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext
import auth.User

class RemindersResourceHandlerSpec extends PlaySpec with IdiomaticMockito with ScalaFutures {

  implicit val ec = ExecutionContext.global

  "listReminders" should {
    "request the reminders from the repository" in {
      WithTestContext() { c =>
        c.repo.list(c.listReq) shouldReturn Future.successful(
          Seq(Fixtures.aReminder)
        )
        c.repo.count(c.listReq) shouldReturn Future.successful(10)
        val result = c.handler.listReminders(c.listReq).futureValue
        result must equal(ListReminderResponse(Seq(Fixtures.aReminder), 2, 10))
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

  "deleteReminder" should {
    "send a deletion request to repository if exists" in {
      WithTestContext() { c =>
        val deleteReq = DeleteReminderRequest(1, Fixtures.anUser)
        c.repo.read(1, Fixtures.anUser) shouldReturn Future.successful(Some(Fixtures.aReminder))
        c.repo.delete(deleteReq) shouldReturn Future.successful(())
        val result = c.handler.deleteReminder(deleteReq).futureValue
        result must equal(())
      }
    }

    "throw Not Found if not exists" in {
      WithTestContext() { c =>
        val deleteReq = DeleteReminderRequest(1, Fixtures.anUser)
        c.repo.read(1, Fixtures.anUser) shouldReturn Future.successful(None)
        val result = c.handler.deleteReminder(deleteReq).failed.futureValue
        result must equal(new ReminderNotFound)
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
          listReq = ListReminderRequest(Fixtures.anUser, 1, 2),
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
