package auth

import org.scalatestplus.play._
import play.api.test._

import functional.utils.TestUtils._
import functional.utils.WithTestApp
import play.api.libs.ws.WSClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Seconds
import org.scalatest.time.Millis
import org.scalatest.time.Span
import play.api.libs.json.Json
import play.api.Application

class AuthFunSpec extends PlaySpec with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  "Token flow" should {

    "get a token for an user" in {
      WithUnloggedUserContext { c =>
        val postResult = c
          .request(s"/api/auth/token")
          .withBody(Json.obj("email" -> c.email, "password" -> c.password))
          .execute("POST")
          .futureValue
        postResult.status mustBe 200
        postResult.json must equal(
          Json.obj(
            "value" -> c.token,
            "expiresAt" -> c.expiresAt
          )
        )
      }
    }

    "get a 200 on the ping endpoint if logged in" in {
      WithUnloggedUserContext { c =>
        val getResult = c
          .request(s"/api/auth/ping")
          .withHttpHeaders("Authorization" -> s"Bearer ${c.token}")
          .execute()
          .futureValue
        getResult.status mustBe 204
      }
    }

    "get a 401 if invalid token" in {
      WithUnloggedUserContext { c =>
        val getResult = c
          .request("/api/auth/ping")
          .withHttpHeaders("Authorization" -> s"Bearer FALSETOKEN")
          .execute()
          .futureValue
        getResult.status mustBe 401
      }
    }
  }

  "Create user flow" should {

    "create an user, get a token, and ping" in {
      WithUnloggedUserContext { c =>
        val newUserEmail = "new@user.email"
        val newUserPass = "newUserPass"
        val createResult = c
          .request(s"/api/auth/user")
          .withBody(Json.obj("email" -> newUserEmail, "password" -> newUserPass))
          .execute("POST")
          .futureValue
        createResult.status must equal(201)
        //FakeUserRepository increases ids by 1
        val expectedId = c.id + 1
        createResult.json must equal(
          Json.obj("id" -> expectedId, "email" -> newUserEmail)
        )

        val tokenResult = c
          .request("/api/auth/token")
          .withBody(Json.obj("email" -> newUserEmail, "password" -> newUserPass))
          .execute("POST")
          .futureValue
        tokenResult.status must equal(200)

        val token = (tokenResult.json \ "value").as[String]
        val pingResult = c
          .request("/api/auth/ping")
          .withHttpHeaders("Authorization" -> s"Bearer $token")
          .execute()
          .futureValue
        pingResult.status must equal(204)
      }
    }

  }

}

case class UnloggedUserContext(
    app: Application,
    email: String,
    password: String,
    token: String,
    expiresAt: String,
    id: Int
) {
  def request(url: String) =
    app.injector.instanceOf[WSClient].url(s"${testServerUrl}${url}")
}

object WithUnloggedUserContext {

  lazy val id = 123
  lazy val email = "a@b.c"
  lazy val password = "abc"
  lazy val token = "TOKEN"
  lazy val expiresAt = "2020-10-11T00:00:00.000Z"
  lazy val now = "2020-01-11T00:00:00.000Z"
  lazy val appConf = Map(
    "auth.fakeUser.id" -> id,
    "auth.fakeUser.email" -> email,
    "auth.fakeUser.password" -> password,
    "auth.fakeToken.value" -> token,
    "auth.fakeToken.expiresAt" -> expiresAt,
    "services.clock.now" -> now
  )

  def apply(block: UnloggedUserContext => Any) = {
    WithTestApp(appConf) { app =>
      Helpers.running(TestServer(testServerPort, app)) {
        val context =
          UnloggedUserContext(app, email, password, token, expiresAt, id)
        block(context)
      }
    }
  }
}
