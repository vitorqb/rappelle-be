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

  "create" should {

    "send request to tokenRepo and return the result" in {
      val user = User(1, "a@b.c")
      val userRepo = mock[UserRepositoryLike]
      userRepo.read(user.email) shouldReturn Future.successful(Some(user))
      val tokenRepo = mock[AuthTokenRepositoryLike]
      val expiresAt = DateTime.parse("2021-12-25")
      val tokenGenerator = new FakeTokenGenerator("sometoken", expiresAt)
      val handler = new AuthResourceHandler(tokenRepo, userRepo, tokenGenerator)
      val input = CreateTokenRequestInput(user.email, "pass")
      val request =
        CreateTokenRequest(user, expiresAt, "sometoken")
      val token = Token(request.value, request.expiresAt, user.id)
      tokenRepo.create(request) shouldReturn Future.successful(token)

      val result = handler.createToken(input)

      result.futureValue mustEqual token
      tokenRepo.create(request) wasCalled once
    }

  }

}
