package auth

import org.scalatestplus.play.PlaySpec
import org.mockito.IdiomaticMockito
import scala.concurrent.Future
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import services.ClockLike
import services.EmailSvcLike
import scala.concurrent.ExecutionContext
import org.mockito.ArgumentMatchersSugar

class EmailConfirmationSvcSpec
    extends PlaySpec
    with IdiomaticMockito
    with ArgumentMatchersSugar
    with ScalaFutures {

  implicit val ec = ExecutionContext.global

  "send" should {
    "create and entry on the repo and send an email" in {
      WithTestContext() { c =>
        c.clock.now() shouldReturn c.datetime
        c.keyGenerator.genValue(*) shouldReturn c.key
        val confirmationRequest =
          CreateEmailConfirmationRequest(c.user.id, c.key, c.datetime)
        c.repo.create(confirmationRequest) shouldReturn Future.successful(
          mock[EmailConfirmation]
        )

        c.service.send(c.user)

        c.repo.create(confirmationRequest) wasCalled once
        c.emailSvc.send(c.user.email, *, *) wasCalled once
      }
    }
  }

  "confirm" should {
    "update entry on repository" in {
      WithTestContext() { c =>
        c.clock.now() shouldReturn c.oldEmailConfirmation.sentAt.plusSeconds(1)
        val updateRequest = c.updateRequest(Some(c.clock.now()))
        c.repo.read(c.key) shouldReturn Future.successful(
          Some(c.oldEmailConfirmation)
        )
        c.repo.update(updateRequest) shouldReturn Future.successful(
          Some(c.newEmailConfirmation)
        )
        val result = c.service.confirm(c.confirmationRequest).futureValue
        result must equal(
          SuccessEmailConfirmationResult(c.user.id)
        )
      }
    }
    "fail to update if expired" in {
      WithTestContext() { c =>
        c.clock.now() shouldReturn c.oldEmailConfirmation.sentAt.plusMinutes(
          c.expirationSeconds
        )
        c.repo.read(c.key) shouldReturn Future.successful(
          Some(c.oldEmailConfirmation)
        )
        val result = c.service.confirm(c.confirmationRequest).futureValue
        result must equal(ExpiredKeyEmailConfirmationResult())
      }
    }
    "fail to update if already confirmed" in {
      WithTestContext() { c =>
        c.clock.now() shouldReturn c.oldEmailConfirmation.sentAt.plusSeconds(1)
        c.repo.read(c.key) shouldReturn Future.successful(
          Some(c.newEmailConfirmation)
        )
        val result = c.service.confirm(c.confirmationRequest).futureValue
        result must equal(InvalidKeyEmailConfirmationResult())
      }
    }
    "fail to update if invalid key" in {
      WithTestContext() { c =>
        c.repo.read(c.key) shouldReturn Future.successful(None)
        val result = c.service.confirm(c.confirmationRequest).futureValue
        result must equal(InvalidKeyEmailConfirmationResult())
      }
    }
  }

  case class TestContext(
      datetime: DateTime,
      user: User,
      key: String,
      keyGenerator: TokenGeneratorLike,
      emailSvc: EmailSvcLike,
      oldEmailConfirmation: EmailConfirmation,
      newEmailConfirmation: EmailConfirmation,
      confirmationRequest: EmailConfirmationRequest,
      repo: EmailConfirmationRepositoryLike,
      clock: ClockLike,
      expirationSeconds: Int,
      service: EmailConfirmationSvc
  ) {
    def updateRequest(expiresAt: Option[DateTime]) =
      UpdateEmailConfirmationRequest(oldEmailConfirmation.id, Some(expiresAt))
  }

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      val repo = mock[EmailConfirmationRepositoryLike]
      val key = "somekey"
      val oldEmailConfirmation =
        EmailConfirmation(1, 1, key, DateTime.parse("2020-01-01"), None)
      val newEmailConfirmation =
        oldEmailConfirmation.copy(
          responseReceivedAt = Some(DateTime.parse("2020-01-02"))
        )
      val emailSvc = mock[EmailSvcLike]
      val keyGenerator = mock[TokenGeneratorLike]
      val clock = mock[ClockLike]
      val expirationSeconds = 3600
      block(
        TestContext(
          datetime = DateTime.parse("2020-01-01"),
          user = User(1, "foo@bar.baz", false),
          emailSvc = emailSvc,
          keyGenerator = keyGenerator,
          key = key,
          oldEmailConfirmation = oldEmailConfirmation,
          newEmailConfirmation = newEmailConfirmation,
          repo = repo,
          confirmationRequest = EmailConfirmationRequest(key),
          service = new EmailConfirmationSvc(
            repo,
            clock,
            expirationSeconds,
            keyGenerator,
            emailSvc
          ),
          clock = clock,
          expirationSeconds = expirationSeconds
        )
      )
    }
  }

}
