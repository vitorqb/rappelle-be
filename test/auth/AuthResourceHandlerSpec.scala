package auth

import org.scalatestplus.play.PlaySpec
import org.mockito.IdiomaticMockito
import org.mockito.ArgumentMatchersSugar
import org.joda.time.DateTime
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext
import play.api.test.FakeRequest

class AuthResourceHandlerSpec
    extends PlaySpec
    with IdiomaticMockito
    with ArgumentMatchersSugar
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
        val user = User(999, request.email, true)
        c.userRepo.read(request.email) shouldReturn Future.successful(None)
        c.userRepo.create(request) shouldReturn Future.successful(user)
        c.emailConfirmationSvc.send(*, *) shouldReturn Future.successful(())

        val result =
          c.handler.createUser(requestInput)(FakeRequest()).futureValue

        result must equal(user)
        c.userRepo.create(request) wasCalled once
        c.emailConfirmationSvc.send(user, *) wasCalled once
      }
    }

    "fail if user already exists" in {
      WithTestContext() { c =>
        val requestInput = CreateUserRequestInput(c.user.email, "pass")
        c.userRepo.read(c.user.email) shouldReturn Future.successful(
          Some(c.user)
        )

        val result =
          c.handler.createUser(requestInput)(FakeRequest()).failed.futureValue

        result mustBe a[UserAlreadyExists]
        c.userRepo.create(*) wasCalled 0.times
      }
    }

  }

  "createEmailConfirmation" should {
    "send msg to user repo and " in {
      WithCreateEmailConfirmationTestContext() { c =>
        c.emailConfirmationSvc.confirm(c.request) shouldReturn Future
          .successful(
            SuccessEmailConfirmationResult(c.newUser.id)
          )
        c.userRepo.update(c.updateUserRequest) shouldReturn Future.successful(
          Some(c.newUser)
        )
        val result = c.handler.confirmEmail(c.request).futureValue
        result must equal(SuccessEmailConfirmationResult(c.newUser.id))
        c.userRepo.update(c.updateUserRequest) wasCalled once
      }
    }
    "return value from confirmation svc" in {
      WithCreateEmailConfirmationTestContext() { c =>
        c.emailConfirmationSvc.confirm(c.request) shouldReturn Future
          .successful(
            SuccessEmailConfirmationResult(c.newUser.id)
          )
        c.userRepo.update(c.updateUserRequest) shouldReturn Future.successful(
          Some(c.newUser)
        )
        val result = c.handler.confirmEmail(c.request).futureValue
        result must equal(SuccessEmailConfirmationResult(c.newUser.id))
        c.userRepo.update(c.updateUserRequest) wasCalled once
      }
    }
    "throw user does not exist if user is missing" in {
      WithCreateEmailConfirmationTestContext() { c =>
        c.emailConfirmationSvc.confirm(c.request) shouldReturn Future
          .successful(
            SuccessEmailConfirmationResult(c.newUser.id)
          )
        c.userRepo.update(c.updateUserRequest) shouldReturn Future.successful(
          None
        )
        val result = c.handler.confirmEmail(c.request).failed.futureValue
        result mustBe a[UserDoesNotExist]
      }
    }
  }

  case class TestContext(
      user: User,
      userRepo: UserRepositoryLike,
      tokenRepo: AuthTokenRepositoryLike,
      expiresAt: DateTime,
      emailConfirmationSvc: EmailConfirmationSvcLike,
      handler: AuthResourceHandler
  )

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      val userRepo = mock[UserRepositoryLike]
      val tokenRepo = mock[AuthTokenRepositoryLike]
      val expiresAt = DateTime.parse("2021-12-25")
      val tokenGenerator = new FakeTokenGenerator("sometoken", expiresAt)
      val emailConfirmationSvc = mock[EmailConfirmationSvcLike]
      block(
        TestContext(
          user = User(1, "a@b.c", true),
          userRepo = userRepo,
          tokenRepo = tokenRepo,
          expiresAt = expiresAt,
          emailConfirmationSvc = emailConfirmationSvc,
          handler = new AuthResourceHandler(
            tokenRepo,
            userRepo,
            tokenGenerator,
            emailConfirmationSvc
          )
        )
      )
    }
  }

  case class CreateEmailConfirmationTestContext(
      oldUser: User,
      newUser: User,
      request: EmailConfirmationRequest,
      updateUserRequest: UpdateUserRequest,
      emailConfirmationSvc: EmailConfirmationSvcLike,
      userRepo: UserRepositoryLike,
      handler: AuthResourceHandler
  )

  object WithCreateEmailConfirmationTestContext {
    def apply()(block: CreateEmailConfirmationTestContext => Any) = {
      WithTestContext() { c =>
        val oldUser = c.user.copy(emailConfirmed = false)
        block(
          CreateEmailConfirmationTestContext(
            oldUser = oldUser,
            newUser = oldUser.copy(emailConfirmed = true),
            updateUserRequest =
              UpdateUserRequest(oldUser.id, emailConfirmed = Some(true)),
            request = EmailConfirmationRequest("somekey"),
            emailConfirmationSvc = c.emailConfirmationSvc,
            userRepo = c.userRepo,
            handler = c.handler
          )
        )
      }
    }
  }

}
