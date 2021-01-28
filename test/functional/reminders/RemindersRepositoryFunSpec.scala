package reminders

import functional.utils.FunctionalSpec
import testutils.Fixtures
import functional.utils.WithTestApp
import functional.utils.WithTestDb
import services.FakeUniqueIdGenerator

class RemindersRepositoryFunSpec extends FunctionalSpec {

  "create and list" should {
    "" in {
      WithTestContext() { c =>
        val reminder = c.repo.create(c.createReq).futureValue
        reminder must equal(Fixtures.aReminder.copy(id=1))
        val listReq = ListReminderRequest(Fixtures.anUser)
        val list = c.repo.list(listReq).futureValue
        list must equal(Seq(reminder))
      }
    }

    "reads only specified number of items" in {
      WithTestContext() { c =>
        c.repo.create(c.createReq).futureValue
        c.repo.create(c.createReq.copy(title="2")).futureValue
        val reqPage1 = ListReminderRequest(Fixtures.anUser, itemsPerPage = 1, page = 1)
        c.repo.list(reqPage1).futureValue must equal(Seq(Fixtures.aReminder.copy(id=2, title="2")))
        val reqPage2 = ListReminderRequest(Fixtures.anUser, itemsPerPage = 1, page = 2)
        c.repo.list(reqPage2).futureValue must equal(Seq(Fixtures.aReminder.copy(id=1)))
      }
    }

  }

  "count" should {
    "count the number of items" in {
      WithTestContext() { c =>
        val req = ListReminderRequest(Fixtures.anUser, itemsPerPage = 1, page = 1)
        c.repo.count(req).futureValue must equal(0)
        c.repo.create(c.createReq).futureValue
        c.repo.count(req).futureValue must equal(1)
        c.repo.create(c.createReq).futureValue
        c.repo.count(req).futureValue must equal(2)
        c.repo.create(c.createReq).futureValue
        c.repo.count(req).futureValue must equal(3)
      }
    }
  }

  case class TestContext(
    createReq: CreateReminderRequest,
      repo: RemindersRepository
  )

  object WithTestContext {

    def apply()(block: TestContext => Any): Any = {
      WithTestApp() { app =>
        WithTestDb(app) { db =>
          block(
            TestContext(
              createReq = CreateReminderRequest(
                Fixtures.anUser,
                Fixtures.aReminder.title,
                Fixtures.aReminder.datetime
              ),
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
