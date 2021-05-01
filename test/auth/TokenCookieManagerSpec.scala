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
import services.FakeEncriptionSvc
import services.EncryptionSvcLike
import java.util.Base64

class TokenCookieManagerSpec
    extends PlaySpec
    with IdiomaticMockito
    with ArgumentMatchersSugar
    with ScalaFutures {

  implicit val ec = ExecutionContext.global
  import TokenCookieManagerLike._

  "extractToken" should {

    "return missing cookie if no cookie" in {
      WithTestContext() { c =>
        val request = FakeRequest()
        c.manager.extractToken(request).futureValue must equal(
          MissingCookie()
        )
      }
    }

    "return token not found if invalid cookie value" in {
      WithTestContext() { c =>
        val request =
          FakeRequest().withCookies(Cookie(COOKIE_NAME, c.encryptedToken))
        c.tokenRepo.read("token") shouldReturn Future.successful(None)
        c.manager.extractToken(request).futureValue must equal(
          TokenNotFound()
        )
      }
    }

    "return invalid token if token is found but invalid" in {
      WithTestContext() { c =>
        val token = mock[Token]
        token.isValid(*) shouldReturn false
        val request =
          FakeRequest().withCookies(Cookie(COOKIE_NAME, c.encryptedToken))
        c.tokenRepo.read("token") shouldReturn Future.successful(Some(token))
        c.manager.extractToken(request).futureValue must equal(InvalidToken())
      }
    }

    "return found if valid token" in {
      WithTestContext() { c =>
        val token = mock[Token]
        token.isValid(*) shouldReturn true
        val request =
          FakeRequest().withCookies(Cookie(COOKIE_NAME, c.encryptedToken))
        c.tokenRepo.read("token") shouldReturn Future.successful(Some(token))
        c.manager.extractToken(request).futureValue must equal(Found(token))
      }
    }

  }

  "cookie" should {
    "generate a cookie" in {
      WithTestContext() { c =>
        val token       = Token("token", DateTime.parse("2020-01-01"), 1)
        val cookie      = c.manager.cookie(token)
        val cookieValue = Base64.getDecoder().decode(cookie.value)
        c.encryptionSvc.decrypt(cookieValue) must equal("token")
      }
    }
  }

  case class TestContext(
      manager: TokenCookieManager,
      tokenRepo: AuthTokenRepositoryLike,
      encryptionSvc: EncryptionSvcLike,
      encryptedToken: String,
      clock: ClockLike
  )

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      val tokenRepo     = mock[AuthTokenRepositoryLike]
      val clock         = new FakeClock(DateTime.parse("2020-01-01T00:00:00"))
      val encryptionSvc = new FakeEncriptionSvc
      val encryptedToken =
        Base64.getEncoder().encodeToString(encryptionSvc.encrypt("token"))
      block(
        TestContext(
          manager = new TokenCookieManager(
            tokenRepo,
            clock,
            encryptionSvc
          ),
          tokenRepo = tokenRepo,
          clock = clock,
          encryptedToken = encryptedToken,
          encryptionSvc = encryptionSvc
        )
      )
    }
  }

}
