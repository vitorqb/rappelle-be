package auth

import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import scala.concurrent.ExecutionContext
import play.api.mvc.Cookie
import org.mockito.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import org.mockito.ArgumentMatchersSugar
import services.ClockLike
import services.FakeClock
import org.joda.time.DateTime
import services.FakePasswordHashSvc

class RequestTokenExtractorSpec
    extends PlaySpec
    with IdiomaticMockito
    with ArgumentMatchersSugar
    with ScalaFutures {

  implicit val ec = ExecutionContext.global
  import RequestTokenExtractorLike._

  "extractToken" should {

    "return missing cookie if no cookie" in {
      WithTestContext() { c =>
        val request = FakeRequest()
        c.extractor.extractToken(request).futureValue must equal(
          MissingCookie()
        )
      }
    }

    "return token not found if invalid cookie value" in {
      WithTestContext() { c =>
        val request = FakeRequest().withCookies(Cookie(COOKIE_NAME, "foo_HASHED"))
        c.tokenRepo.read("foo") shouldReturn Future.successful(None)
        c.extractor.extractToken(request).futureValue must equal(
          TokenNotFound()
        )
      }
    }

    "return invalid token if token is found but invalid" in {
      WithTestContext() { c =>
        val token = mock[Token]
        token.isValid(*) shouldReturn false
        val request = FakeRequest().withCookies(Cookie(COOKIE_NAME, "foo_HASHED"))
        c.tokenRepo.read("foo") shouldReturn Future.successful(Some(token))
        c.extractor.extractToken(request).futureValue must equal(InvalidToken())
      }
    }

    "return found if valid token" in {
      WithTestContext() { c =>
        val token = mock[Token]
        token.isValid(*) shouldReturn true
        val request = FakeRequest().withCookies(Cookie(COOKIE_NAME, "foo_HASHED"))
        c.tokenRepo.read("foo") shouldReturn Future.successful(Some(token))
        c.extractor.extractToken(request).futureValue must equal(Found(token))
      }
    }

  }

  case class TestContext(
      extractor: RequestTokenExtractor,
      tokenRepo: AuthTokenRepositoryLike,
      clock: ClockLike
  )

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      val tokenRepo = mock[AuthTokenRepositoryLike]
      val clock = new FakeClock(DateTime.parse("2020-01-01T00:00:00"))
      block(
        TestContext(
          extractor = new RequestTokenExtractor(
            tokenRepo,
            clock,
            new FakePasswordHashSvc
          ),
          tokenRepo = tokenRepo,
          clock = clock
        )
      )
    }
  }

}
