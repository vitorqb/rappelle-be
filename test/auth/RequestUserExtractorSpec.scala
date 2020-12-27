package auth

import org.scalatestplus.play.PlaySpec
import org.mockito.IdiomaticMockito
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.joda.time.DateTime
import play.api.test.FakeRequest
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Results
import services.ClockLike

class RequestUserExtractorSpec
    extends PlaySpec
    with IdiomaticMockito
    with ScalaFutures
    with Results {

  implicit val ec = ExecutionContext.global

  "extractUser" should {

    "find an user by token" in {
      WithTestContext() { c =>
        c.userRepo.read(c.user.id) shouldReturn Future.successful(Some(c.user))
        c.tokenRepo.read(c.token.value) shouldReturn Future.successful(
          Some(c.token)
        )
        val request = FakeRequest().withHeaders(
          "Authorization" -> s"Bearer ${c.token.value}"
        )
        val result = c.extractor.extractUser(request).futureValue
        result must equal(Some(c.user))
      }
    }

    "not find an user for a wrong token" in {
      WithTestContext() { c =>
        c.userRepo.read(c.user.id) shouldReturn Future.successful(Some(c.user))
        c.tokenRepo.read("WRONG_TOKEN") shouldReturn Future.successful(None)
        val request =
          FakeRequest().withHeaders("Authorization" -> s"Bearer WRONG_TOKEN")
        val result = c.extractor.extractUser(request).futureValue
        result must equal(None)
      }
    }

    "not find an user for a deleted user" in {
      WithTestContext() { c =>
        c.userRepo.read(c.user.id) shouldReturn Future.successful(None)
        c.tokenRepo.read(c.token.value) shouldReturn Future.successful(
          Some(c.token)
        )
        val request =
          FakeRequest().withHeaders(
            "Authorization" -> s"Bearer ${c.token.value}"
          )
        val result = c.extractor.extractUser(request).futureValue
        result must equal(None)
      }
    }

    "not find an user for a missing header" in {
      WithTestContext() { c =>
        val request = FakeRequest()
        val result = c.extractor.extractUser(request).futureValue
        result must equal(None)
      }
    }

    "not find an user for a misspelled token" in {
      WithTestContext() { c =>
        c.userRepo.read(c.user.id) shouldReturn Future.successful(None)
        c.tokenRepo.read(c.token.value) shouldReturn Future.successful(
          Some(c.token)
        )
        val request =
          FakeRequest().withHeaders("Authorization" -> s"FOO ${c.token.value}")
        val result = c.extractor.extractUser(request).futureValue
        result must equal(None)
      }
    }

    "not find an user for an expired token" in {
      WithTestContext() { c =>
        c.clock.now() shouldReturn c.token.expiresAt.plusDays(1)
        c.userRepo.read(c.user.id) shouldReturn Future.successful(Some(c.user))
        c.tokenRepo.read(c.token.value) shouldReturn Future.successful(
          Some(c.token)
        )
        val request =
          FakeRequest().withHeaders(
            "Authorization" -> s"Bearer ${c.token.value}"
          )
        val result = c.extractor.extractUser(request).futureValue
        result must equal(None)
      }
    }
  }

  "withUser" should {

    "return unauthorized if no user" in {
      WithTestContext() { c =>
        val result =
          c.extractor.withUser(FakeRequest())(_ => Future.successful(Ok))
        result.futureValue must equal(Unauthorized)
      }
    }

    "call block with the user" in {
      WithTestContext() { c =>
        c.userRepo.read(c.user.id) shouldReturn Future.successful(Some(c.user))
        c.tokenRepo.read(c.token.value) shouldReturn Future.successful(
          Some(c.token)
        )
        val request = FakeRequest().withHeaders(
          "Authorization" -> s"Bearer ${c.token.value}"
        )
        val result =
          c.extractor.withUser(request)(u => Future.successful(Ok(u.email)))
        result.futureValue must equal(Ok(c.user.email))
      }
    }

  }

  case class TestContext(
      user: User,
      token: Token,
      userRepo: UserRepositoryLike,
      tokenRepo: AuthTokenRepositoryLike,
      extractor: RequestUserExtractorLike,
      clock: ClockLike
  )

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      val clock = mock[ClockLike]
      val userRepo = mock[UserRepositoryLike]
      val tokenRepo = mock[AuthTokenRepositoryLike]
      val extractor = new RequestUserExtractor(userRepo, tokenRepo, clock)
      val user = User(123, "a@b.c")
      val token = Token("tokenvalue", DateTime.parse("2022-12-1"), user.id)
      block(TestContext(user, token, userRepo, tokenRepo, extractor, clock))
    }
  }

}
