package auth

import org.scalatestplus.play.PlaySpec
import org.joda.time.DateTime
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.libs.json.Json
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import org.mockito.IdiomaticMockito
import scala.concurrent.Future
import org.mockito.ArgumentMatchersSugar
import play.api.mvc.Results
import play.api.test.Helpers

class AuthControllerSpec
    extends PlaySpec
    with IdiomaticMockito
    with ArgumentMatchersSugar
    with Results {

  implicit val sys = ActorSystem("test")
  implicit val ec: ExecutionContext = ExecutionContext.global

  import AuthJsonSerializers._

  "postToken" should {

    "pass a CreateTokenRequest to the resource handler" in {
      WithTestContext() { c =>
        (c.resourceHandler.createToken(c.createTokenRequest)
          returns
            Future.successful(c.token))
        val request = FakeRequest().withBody(Json.toJson(c.createTokenRequest))
        val result = c.controller.postToken()(request)
        status(result) mustBe 200
        contentAsJson(result) mustBe Json.toJson(c.token)
      }
    }

    "return 400 if user not found" in {
      WithTestContext() { c =>
        c.resourceHandler.createToken(any) throws new UserDoesNotExist
        val request = FakeRequest().withBody(Json.toJson(c.createTokenRequest))
        val result = c.controller.postToken()(request)
        status(result) mustBe 400
        contentAsJson(result) mustBe Json.obj("msg" -> "User does not exist")
      }
    }
  }

  "ping" should {
    "return 400 on invalid token" in {
      WithTestContext((_: User) => None) { c =>
        val request = FakeRequest(routes.AuthController.ping())
        val result = c.controller.ping()(request)
        Helpers.status(result) mustEqual UNAUTHORIZED
      }
    }
    "return 200 on valid token" in {
      WithTestContext() { c =>
        val request = FakeRequest(routes.AuthController.ping())
        val result = c.controller.ping()(request)
        Helpers.status(result) mustEqual NO_CONTENT
      }
    }
  }

  "postUser" should {
    "return created user" in {
      WithTestContext() { c =>
        val createUserReqInput =
          CreateUserRequestInput("new@user.com", "new_password")
        val user = User(1, createUserReqInput.email, true)
        val body = Json.toJson(createUserReqInput)
        val request =
          FakeRequest(routes.AuthController.postUser()).withBody(body)
        c.resourceHandler.createUser(createUserReqInput)(
          request
        ) shouldReturn Future
          .successful(user)
        val result = c.controller.postUser()(request)
        Helpers.status(result) must equal(CREATED)
        Helpers.contentAsJson(result) must equal(
          Json.obj(
            "id" -> 1,
            "email" -> createUserReqInput.email,
            "isActive" -> true
          )
        )
      }
    }
    "returns error if user already exists" in {
      WithTestContext() { c =>
        val createUserReqInput = CreateUserRequestInput(c.user.email, "pass")
        val body = Json.toJson(createUserReqInput)
        val request =
          FakeRequest(routes.AuthController.postUser()).withBody(body)
        c.resourceHandler.createUser(createUserReqInput)(
          request
        ) shouldReturn Future
          .failed(new UserAlreadyExists)
        val result = c.controller.postUser()(request)
        Helpers.status(result) must equal(BAD_REQUEST)
        Helpers.contentAsJson(result) must equal(
          Json.obj("msg" -> "An user with this email already exists.")
        )
      }
    }
  }

  "recoverToken" should {
    "return 400 if no cookie" in {
      WithTestContext() { c =>
        val request = FakeRequest(routes.AuthController.recoverToken())
        c.tokenCookieManager.extractToken(request) shouldReturn Future
          .successful(TokenCookieManagerLike.MissingCookie())
        val result = c.controller.recoverToken()(request)
        Helpers.status(result) must equal(400)
        Helpers.contentAsJson(result) must equal(
          Json.obj("msg" -> "Missing cookie")
        )
      }
    }
    "return 400 if token not found" in {
      WithTestContext() { c =>
        val request = FakeRequest(routes.AuthController.recoverToken())
        c.tokenCookieManager.extractToken(request) shouldReturn Future
          .successful(TokenCookieManagerLike.TokenNotFound())
        val result = c.controller.recoverToken()(request)
        Helpers.status(result) must equal(400)
        Helpers.contentAsJson(result) must equal(
          Json.obj("msg" -> "Invalid cookie")
        )
      }
    }
    "return 400 if invalid token" in {
      WithTestContext() { c =>
        val request = FakeRequest(routes.AuthController.recoverToken())
        c.tokenCookieManager.extractToken(request) shouldReturn Future
          .successful(TokenCookieManagerLike.InvalidToken())
        val result = c.controller.recoverToken()(request)
        Helpers.status(result) must equal(400)
        Helpers.contentAsJson(result) must equal(
          Json.obj("msg" -> "Found invalid token")
        )
      }
    }
    "return 200 with token" in {
      WithTestContext() { c =>
        val request = FakeRequest(routes.AuthController.recoverToken())
        c.tokenCookieManager.extractToken(request) shouldReturn Future
          .successful(TokenCookieManagerLike.Found(c.token))
        val result = c.controller.recoverToken()(request)
        Helpers.status(result) must equal(200)
        Helpers.contentAsJson(result) must equal(Json.toJson(c.token))
      }
    }
  }

  "postEmailConfirmation" should {
    "return 201 if email confirmation is a success" in {
      WithTestContext() { c =>
        val request = EmailConfirmationRequest(c.emailConfirmationKey)
        val handlerResponse =
          Future.successful(SuccessEmailConfirmationResult(c.user.id))
        c.resourceHandler.confirmEmail(
          request
        ) shouldReturn handlerResponse
        val body = Json.toJson(request)
        val httpRequest =
          FakeRequest(routes.AuthController.postEmailConfirmation())
            .withBody(body)
        val result = c.controller.postEmailConfirmation()(httpRequest)
        Helpers.status(result) must equal(NO_CONTENT)
      }
    }
    "return 400 if email confirmation fails because key has expired" in {
      WithTestContext() { c =>
        val request = EmailConfirmationRequest(c.emailConfirmationKey)
        val handlerResponse =
          Future.successful(ExpiredKeyEmailConfirmationResult())
        c.resourceHandler.confirmEmail(
          request
        ) shouldReturn handlerResponse
        val body = Json.toJson(request)
        val httpRequest =
          FakeRequest(routes.AuthController.postEmailConfirmation())
            .withBody(body)
        val result = c.controller.postEmailConfirmation()(httpRequest)
        Helpers.status(result) must equal(BAD_REQUEST)
        Helpers.contentAsJson(result) must equal(
          Json.obj(
            "msg" -> "The key has expired"
          )
        )
      }
    }
    "return 400 if email confirmation fails because key is invalid" in {
      WithTestContext() { c =>
        val request = EmailConfirmationRequest(c.emailConfirmationKey)
        val handlerResponse =
          Future.successful(InvalidKeyEmailConfirmationResult())
        c.resourceHandler.confirmEmail(
          request
        ) shouldReturn handlerResponse
        val body = Json.toJson(request)
        val httpRequest =
          FakeRequest(routes.AuthController.postEmailConfirmation())
            .withBody(body)
        val result = c.controller.postEmailConfirmation()(httpRequest)
        Helpers.status(result) must equal(BAD_REQUEST)
        Helpers.contentAsJson(result) must equal(
          Json.obj(
            "msg" -> "The key is invalid"
          )
        )
      }
    }
  }

  case class TestContext(
      createTokenRequest: CreateTokenRequestInput,
      resourceHandler: AuthResourceHandlerLike,
      token: Token,
      controller: AuthController,
      user: User,
      requestUserExtractor: RequestUserExtractorLike,
      tokenCookieManager: TokenCookieManagerLike,
      emailConfirmationKey: String
  )

  object WithTestContext {

    def apply()(block: TestContext => Any): Any =
      apply((u: User) => Some(u))(block)

    def apply(userFn: User => Option[User])(block: TestContext => Any): Any = {
      val emailConfirmationKey = "emailconfirmationkey"
      val email = "a@b.c"
      val user = User(1, email, true)
      val password = "foo"
      val createTokenRequest = CreateTokenRequestInput(email, password)
      val token = Token("value", DateTime.parse("2020-01-01"), user.id)
      val resourceHandler = mock[AuthResourceHandlerLike]
      val requestUserExtractor = new FakeRequestUserExtractor(userFn(user))
      val tokenCookieManager = mock[TokenCookieManagerLike]
      val controller = new AuthController(
        stubControllerComponents(),
        resourceHandler,
        requestUserExtractor,
        tokenCookieManager
      )
      val context =
        TestContext(
          createTokenRequest,
          resourceHandler,
          token,
          controller,
          user,
          requestUserExtractor,
          tokenCookieManager,
          emailConfirmationKey
        )
      block(context)
    }
  }

}
