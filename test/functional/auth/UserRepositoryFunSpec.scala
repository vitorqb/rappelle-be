package auth

import org.scalatestplus.play.PlaySpec
import functional.utils.WithTestApp
import functional.utils.WithTestDb
import org.scalatest.concurrent.ScalaFutures
import services.FakeUniqueIdGenerator
import scala.concurrent.ExecutionContext
import services.FakePasswordHashSvc
import org.scalatest.time.Span
import org.scalatest.time.Seconds
import org.scalatest.time.Millis

class UserRepositoryFunSpec extends PlaySpec with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))
  implicit val ec = ExecutionContext.global

  "create and read an user" in {
    WithTestContext() { c =>
      c.repo.create(c.request).futureValue
      val result = c.repo.read(c.request.email).futureValue.get
      result.email mustEqual c.request.email
      result.id must equal(1)
    }
  }

  "return None if no user" in {
    WithTestContext() { c =>
      c.repo.read("foo").futureValue must equal(None)
    }
  }

  "validate user password" in {
    WithTestContext() { c =>
      val user = c.repo.create(c.request).futureValue
      c.repo.passwordIsValid(user, "foo").futureValue must equal(false)
      c.repo.passwordIsValid(user, c.request.password).futureValue must equal(
        true
      )
    }
  }

  case class TestContext(request: CreateUserRequest, repo: UserRepositoryLike)

  object WithTestContext {
    val request = CreateUserRequest("a@b.c", "password")
    def apply()(block: TestContext => Any): Any = {
      WithTestApp() { app =>
        WithTestDb(app) { db =>
          val idGenerator = new FakeUniqueIdGenerator
          val hashSvc = new FakePasswordHashSvc
          val repo = new UserRepository(db, idGenerator, hashSvc)
          val context = TestContext(request, repo)
          block(context)
        }
      }
    }
  }
}
