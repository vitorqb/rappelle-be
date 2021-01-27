package reminders

import functional.utils.FunctionalSpec
import org.joda.time.DateTime
import testutils.Fixtures
import functional.utils.WithTestApp
import functional.utils.WithTestDb
import services.FakeUniqueIdGenerator

class RemindersRepositoryFunSpec extends FunctionalSpec {

  "create and list" should {
    "" in {
      WithTestContext() { c =>
        val createReq = CreateReminderRequest(
          Fixtures.anUser,
          "title",
          DateTime.parse("2020-01-01")
        )
        val reminder = c.repo.create(createReq).futureValue
        reminder must equal(
          Reminder(1, "title", DateTime.parse("2020-01-01"))
        )
        val listReq = ListReminderRequest(Fixtures.anUser)
        val list = c.repo.list(listReq).futureValue
        list must equal(Seq(reminder))
      }
    }
  }

  case class TestContext(
      repo: RemindersRepository
  )

  object WithTestContext {

    def apply()(block: TestContext => Any): Any = {
      WithTestApp() { app =>
        WithTestDb(app) { db =>
          block(
            TestContext(
              repo = new RemindersRepository(
                db,
                new FakeUniqueIdGenerator
              )
            )
          )
        }
      }
    }

  }

}
