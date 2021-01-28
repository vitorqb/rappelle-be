package auth

import scala.concurrent.Future
import services.ClockLike
import services.EmailSvcLike
import scala.concurrent.ExecutionContext
import play.api.Logger

/** An object that knows how to render a callback url for the confirmation email.
  */
trait CallbackGeneratorLike {
  def render(key: String): String
}

/** Main service used to confirm emails.
  */
trait EmailConfirmationSvcLike {

  def confirm(
      request: EmailConfirmationRequest
  ): Future[EmailConfirmationResult]

  def send(
      user: User,
      callbackGenerator: CallbackGeneratorLike
  ): Future[Unit]
}

/** Base implementation
  */
class EmailConfirmationSvc(
    repo: EmailConfirmationRepositoryLike,
    clock: ClockLike,
    expirationSeconds: Int,
    keyGenerator: TokenGeneratorLike,
    emailSvc: EmailSvcLike
)(implicit
    val ec: ExecutionContext
) extends EmailConfirmationSvcLike {

  val logger                                                           = Logger(getClass())
  val keySize                                                          = 10
  val emailSubject                                                     = "Confirm your Rappelle accoount!"
  def emailBody(key: String, callbackGenerator: CallbackGeneratorLike) = f"""
   |Please click on the following link to confirm your account:
   |
   |  ${callbackGenerator.render(key)}
   |
   |Thanks!
   |""".stripMargin

  override def send(
      user: User,
      callbackGenerator: CallbackGeneratorLike
  ): Future[Unit] = {
    logger.info(f"Sending for user $user")
    val key           = keyGenerator.genValue(keySize)
    val now           = clock.now()
    val createRequest = CreateEmailConfirmationRequest(user.id, key, now)
    repo.create(createRequest).flatMap { _ =>
      emailSvc
        .send(user.email, emailSubject, emailBody(key, callbackGenerator))
        .map(_ => ())
    }
  }

  override def confirm(
      request: EmailConfirmationRequest
  ): Future[EmailConfirmationResult] = {
    val now = clock.now()
    logger.info(f"Handling confirmation request $request at $now")
    repo.read(request.key).flatMap {

      case Some(confirmation) if confirmation.responseReceivedAt.isDefined =>
        Future.successful(InvalidKeyEmailConfirmationResult())

      case Some(confirmation) if confirmation.isValid(now, expirationSeconds) => {
        val updateRequest = UpdateEmailConfirmationRequest(
          confirmation.id,
          responseReceivedAt = Some(Some(now))
        )
        repo.update(updateRequest).map {
          case None    => InvalidKeyEmailConfirmationResult()
          case Some(x) => SuccessEmailConfirmationResult(x.userId)
        }
      }

      case Some(confirmation) if !confirmation.isValid(now, expirationSeconds) =>
        Future.successful(ExpiredKeyEmailConfirmationResult())

      case None =>
        Future.successful(InvalidKeyEmailConfirmationResult())
    }
  }

}

/** Fake implementation
  */
class FakeEmailConfirmationSvc(confirmationKey: String, userId: Int)
    extends EmailConfirmationSvcLike {

  override def send(
      user: User,
      callbackGenerator: CallbackGeneratorLike
  ): Future[Unit] = Future.successful(())

  override def confirm(
      request: EmailConfirmationRequest
  ): Future[EmailConfirmationResult] =
    if (confirmationKey == request.key) {
      Future.successful(SuccessEmailConfirmationResult(userId))
    } else {
      Future.successful(InvalidKeyEmailConfirmationResult())
    }

}
