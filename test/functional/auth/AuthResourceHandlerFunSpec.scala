package auth

import org.scalatestplus.play.PlaySpec
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import functional.utils.WithTestApp
import services.TestEmailSvc
import functional.utils.WithTestDb
import org.scalatest.concurrent.ScalaFutures
import services.EmailSvcLike
import play.api.test.FakeRequest
import org.scalatest.time.Seconds
import org.scalatest.time.Millis
import org.scalatest.time.Span

class AuthResourceHandlerFunSpec extends PlaySpec with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

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
            |  http://localhost/api/auth/emailConfirmationCallback?key=fakeToken
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
      testEmailSvc: TestEmailSvc
  )

  object WithTestContext {

    implicit val ec: ExecutionContext = ExecutionContext.global
    val fakeToken = "fakeToken"
    val expirationDate = "2021-01-12T20:23:39"

    val conf = Map(
      "auth.fakeToken.value" -> fakeToken,
      "auth.fakeToken.expiresAt" -> expirationDate,
      "auth.emailConfirmationSvc.type" -> "EmailConfirmationSvc",
      "auth.emailConfirmationRepository.type" -> "EmailConfirmationRepository",
      "services.email.type" -> "TestEmailSvc"
    )

    def apply(block: TestContext => Any): Any = {
      WithTestApp(conf) { app =>
        WithTestDb(app) { db =>
          block(
            TestContext(
              fakeToken = fakeToken,
              expirationDate = DateTime.parse(expirationDate),
              app.injector.instanceOf[AuthResourceHandler],
              app.injector.instanceOf[EmailSvcLike].asInstanceOf[TestEmailSvc]
            )
          )
        }
      }
    }

  }

}
