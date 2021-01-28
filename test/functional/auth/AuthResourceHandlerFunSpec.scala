package auth

import org.joda.time.DateTime
import functional.utils.WithTestApp
import services.TestEmailSvc
import functional.utils.WithTestDb
import org.scalatest.concurrent.ScalaFutures
import services.EmailSvcLike
import play.api.test.FakeRequest
import functional.utils.FunctionalSpec

class AuthResourceHandlerFunSpec extends FunctionalSpec with ScalaFutures {

  "createUser" should {

    "send an email with the key" in {
      WithTestContext { c =>
        assert(c.testEmailSvc.getSentEmails() == Seq.empty)
        val createUserReq = CreateUserRequestInput("foo@bar.baz", "pass")
        val httpReq = FakeRequest()
        val result = c.handler.createUser(createUserReq)(httpReq).futureValue
        result.email must equal("foo@bar.baz")
        println(c.testEmailSvc)
        c.testEmailSvc.getSentEmails() must equal(
          Seq(
            TestEmailSvc.SentEmail(
              "foo@bar.baz",
              "Confirm your Rappelle accoount!",
              f"""
            |Please click on the following link to confirm your account:
            |
            |  ${c.frontendUrl}/#/emailConfirmation?key=fakeToken
            |
            |Thanks!
            |""".stripMargin
            )
          )
        )
      }
    }

  }

  case class TestContext(
      fakeToken: String,
      expirationDate: DateTime,
      handler: AuthResourceHandler,
      frontendUrl: String,
      testEmailSvc: TestEmailSvc
  )

  object WithTestContext {

    val fakeToken = "fakeToken"
    val expirationDate = "2021-01-12T20:23:39"
    val frontendUrl = "http://127.0.0.1:9000"

    val conf = Map(
      "auth.fakeToken.value" -> fakeToken,
      "auth.fakeToken.expiresAt" -> expirationDate,
      "auth.emailConfirmationSvc.type" -> "EmailConfirmationSvc",
      "auth.emailConfirmationRepository.type" -> "EmailConfirmationRepository",
      "services.email.type" -> "TestEmailSvc",
      "frontendUrl" -> frontendUrl
    )

    def apply(block: TestContext => Any): Any = {
      WithTestApp(conf) { app =>
        WithTestDb(app) { db =>
          block(
            TestContext(
              fakeToken = fakeToken,
              expirationDate = DateTime.parse(expirationDate),
              handler = app.injector.instanceOf[AuthResourceHandler],
              testEmailSvc = app.injector
                .instanceOf[EmailSvcLike]
                .asInstanceOf[TestEmailSvc],
              frontendUrl = frontendUrl
            )
          )
        }
      }
    }

  }

}
