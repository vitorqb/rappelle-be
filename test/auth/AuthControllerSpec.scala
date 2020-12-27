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
        val user = User(1, createUserReqInput.email)
        c.resourceHandler.createUser(createUserReqInput) shouldReturn Future
          .successful(user)
        val body = Json.toJson(createUserReqInput)
        val request =
          FakeRequest(routes.AuthController.postUser()).withBody(body)
        val result = c.controller.postUser()(request)
        Helpers.status(result) must equal(CREATED)
        Helpers.contentAsJson(result) must equal(
          Json.obj("id" -> 1, "email" -> createUserReqInput.email)
        )
      }
    }
    "returns error if user already exists" in {
      WithTestContext() { c =>
        val createUserReqInput = CreateUserRequestInput(c.user.email, "pass")
        c.resourceHandler.createUser(createUserReqInput) shouldReturn Future
          .failed(new UserAlreadyExists)
        val body = Json.toJson(createUserReqInput)
        val request =
          FakeRequest(routes.AuthController.postUser()).withBody(body)
        val result = c.controller.postUser()(request)
        Helpers.status(result) must equal(BAD_REQUEST)
        Helpers.contentAsJson(result) must equal(
          Json.obj("msg" -> "An user with this email already exists.")
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
      requestUserExtractor: RequestUserExtractorLike
  )

  object WithTestContext {

    def apply()(block: TestContext => Any): Any =
      apply((u: User) => Some(u))(block)

    def apply(userFn: User => Option[User])(block: TestContext => Any): Any = {
      val email = "a@b.c"
      val user = User(1, email)
      val password = "foo"
      val createTokenRequest = CreateTokenRequestInput(email, password)
      val token = Token("value", DateTime.parse("2020-01-01"), user.id)
      val resourceHandler = mock[AuthResourceHandlerLike]
      val requestUserExtractor = new FakeRequestUserExtractor(userFn(user))
      val controller = new AuthController(
        stubControllerComponents(),
        resourceHandler,
        requestUserExtractor
      )
      val context =
        TestContext(
          createTokenRequest,
          resourceHandler,
          token,
          controller,
          user,
          requestUserExtractor
        )
      block(context)
    }
  }

}
