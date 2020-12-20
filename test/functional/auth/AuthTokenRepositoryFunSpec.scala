package auth

import org.scalatestplus.play.PlaySpec
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import functional.utils.WithTestApp
import functional.utils.WithTestDb
import scala.concurrent.ExecutionContext
import services.UniqueIdGenerator

class AuthTokenRepositoryFunSpec extends PlaySpec with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "create and get token" in {
    WithTestContext { c =>
      val token = c.repository.create(c.request).futureValue
      token.value mustEqual c.request.value
      token.expiresAt mustEqual c.request.expiresAt
    }
  }

  "create and get two tokens" in {
    WithTestContext { c =>
      val request1 = c.request
      c.repository.create(request1).futureValue

      val request2 = c.request.copy(value = "def456")
      val result2 = c.repository.create(request2).futureValue

      result2.value mustEqual request2.value
    }
  }

  case class TestContext(
      request: CreateTokenRequest,
      repository: AuthTokenRepository
  )

  object WithTestContext {

    implicit val ec: ExecutionContext = ExecutionContext.global
    val value = "abc123"
    val user = User(123, "email@email.email")
    val expiresAt = DateTime.parse("2020-01-01")
    val request = CreateTokenRequest(user, expiresAt, value)

    def apply(block: TestContext => Any): Any = {
      WithTestApp() { app =>
        WithTestDb(app) { db =>
          val idGenerator = new UniqueIdGenerator(db)
          val repository = new AuthTokenRepository(db, idGenerator)
          val context = TestContext(request, repository)
          block(context)
        }
      }
    }
  }
}
