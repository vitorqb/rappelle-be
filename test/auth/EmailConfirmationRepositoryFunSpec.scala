package auth

import org.scalatestplus.play.PlaySpec
import functional.utils.WithTestApp
import functional.utils.WithTestDb
import org.joda.time.DateTime
import services.FakeUniqueIdGenerator
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext

class EmailConfirmationRepositoryFunSpec extends PlaySpec with ScalaFutures {

  implicit val ec = ExecutionContext.global

  "create and read" in {
    WithTestContext() { c =>
      val expEmailConfirmation = EmailConfirmation(
        1,
        c.createReq.userId,
        c.createReq.key,
        c.createReq.sentAt,
        None
      )

      c.repo.read(c.createReq.key).futureValue must equal(None)
      c.repo.create(c.createReq).futureValue must equal(expEmailConfirmation)
      c.repo.read(c.createReq.key).futureValue must equal(
        Some(expEmailConfirmation)
      )
    }
  }

  "update" should {

    "return None if doesnt exist" in {
      WithTestContext() { c =>
        val updateReq = UpdateEmailConfirmationRequest(1)
        c.repo.update(updateReq).futureValue must equal(None)
      }
    }

    "update and retrieve" in {
      WithTestContext() { c =>
        val datetime = DateTime.parse("2022-12-30")
        val updateReq = UpdateEmailConfirmationRequest(
          1,
          responseReceivedAt = Some(Some(datetime))
        )
        val created = c.repo.create(c.createReq).futureValue
        val updated = c.repo.update(updateReq).futureValue

        updated must equal(
          Some(created.copy(responseReceivedAt = Some(datetime)))
        )
        c.repo.read(c.createReq.key).futureValue must equal(updated)
      }
    }
  }

  case class TestContext(
      val idGenerator: FakeUniqueIdGenerator,
      val repo: EmailConfirmationRepositoryLike,
      val createReq: CreateEmailConfirmationRequest
  )

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      WithTestApp() { app =>
        WithTestDb(app) { db =>
          val idGenerator = new FakeUniqueIdGenerator
          block(
            TestContext(
              createReq = CreateEmailConfirmationRequest(
                1,
                "somekey",
                DateTime.parse("2020-01-01")
              ),
              idGenerator = idGenerator,
              repo = new EmailConfirmationRepository(db, idGenerator)
            )
          )
        }
      }
    }
  }

}
