package auth

import org.scalatestplus.play.PlaySpec
import org.mockito.IdiomaticMockito
import org.joda.time.DateTime
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext

class AuthResourceHandlerSpec
    extends PlaySpec
    with IdiomaticMockito
    with ScalaFutures {

  implicit val ec = ExecutionContext.global

  "createToken" should {

    "send request to tokenRepo and return the result" in {
      WithTestContext() { c =>
        c.userRepo.read(c.user.email) shouldReturn Future.successful(
          Some(c.user)
        )
        val input = CreateTokenRequestInput(c.user.email, "pass")
        val request =
          CreateTokenRequest(c.user, c.expiresAt, "sometoken")
        val token = Token(request.value, request.expiresAt, c.user.id)
        c.tokenRepo.create(request) shouldReturn Future.successful(token)

        val result = c.handler.createToken(input)

        result.futureValue mustEqual token
        c.tokenRepo.create(request) wasCalled once
      }
    }

  }

  "createUser" should {

    "send request to userRepo and return the result" in {
      WithTestContext() { c =>
        val requestInput = CreateUserRequestInput("a@b.c", "pass")
        val request =
          CreateUserRequest(requestInput.email, requestInput.password)
        val user = User(999, request.email)
        c.userRepo.create(request) shouldReturn Future.successful(user)

        val result = c.handler.createUser(requestInput).futureValue

        result must equal(user)
        c.userRepo.create(request) wasCalled once
      }
    }

  }

  case class TestContext(
      user: User,
      userRepo: UserRepositoryLike,
      tokenRepo: AuthTokenRepositoryLike,
      expiresAt: DateTime,
      handler: AuthResourceHandler
  )

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      val user = User(1, "a@b.c")
      val userRepo = mock[UserRepositoryLike]
      val tokenRepo = mock[AuthTokenRepositoryLike]
      val expiresAt = DateTime.parse("2021-12-25")
      val tokenGenerator = new FakeTokenGenerator("sometoken", expiresAt)
      val handler = new AuthResourceHandler(tokenRepo, userRepo, tokenGenerator)
      val context = TestContext(user, userRepo, tokenRepo, expiresAt, handler)
      block(context)
    }
  }

}
