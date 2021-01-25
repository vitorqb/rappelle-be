package auth

import org.scalatestplus.play._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Seconds
import org.scalatest.time.Millis
import org.scalatest.time.Span
import play.api.libs.json.Json

import functional.utils.WithAuthContext

class AuthFunSpec extends PlaySpec with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  "Token flow" should {

    "get a token for an user" in {
      WithAuthContext() { c =>
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
      WithAuthContext() { c =>
        val getResult = c
          .requestWithToken(s"/api/auth/ping")
          .execute()
          .futureValue
        getResult.status mustBe 204
      }
    }

    "get a 403 on the ping endpoint if email not confirmed" in {
      WithAuthContext(
        _.updated("auth.fakeUser.emailConfirmed", false)
      ) { c =>
        val getResult = c
          .requestWithToken(s"/api/auth/ping")
          .execute()
          .futureValue
        getResult.status mustBe 403
        getResult.json must equal(Json.obj("msg" -> "User is not active."))
      }
    }

    "get a 401 if invalid token" in {
      WithAuthContext() { c =>
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

    "create an user, confirm email, get a token, and ping" in {
      WithAuthContext() { c =>
        val newUserEmail = "new@user.email"
        val newUserPass = "newUserPass"
        val createResult = c
          .request(s"/api/auth/user")
          .withBody(
            Json.obj("email" -> newUserEmail, "password" -> newUserPass)
          )
          .execute("POST")
          .futureValue
        createResult.status must equal(201)
        //FakeUserRepository increases ids by 1
        val expectedId = c.id + 1
        createResult.json must equal(
          Json.obj(
            "id" -> expectedId,
            "email" -> newUserEmail,
            "isActive" -> false
          )
        )

        val confirmEmailResult = c
          .request("/api/auth/emailConfirmation")
          .withBody(Json.obj("key" -> c.confirmationKey))
          .execute("POST")
          .futureValue
        confirmEmailResult.status must equal(204)

        val tokenResult = c
          .request("/api/auth/token")
          .withBody(
            Json.obj("email" -> newUserEmail, "password" -> newUserPass)
          )
          .execute("POST")
          .futureValue
        tokenResult.status must equal(200)
        val cookie = tokenResult.cookie("RappelleAuth").get

        val token = (tokenResult.json \ "value").as[String]
        val pingResult = c
          .requestWithToken("/api/auth/ping")
          .execute()
          .futureValue
        pingResult.status must equal(204)

        val userResult = c
          .request("/api/auth/token")
          .withCookies(cookie)
          .execute()
          .futureValue
        userResult.json must equal(
          Json.obj("value" -> c.token, "expiresAt" -> c.expiresAt)
        )
      }
    }

  }

}
