package auth

import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Result
import scala.concurrent.Future
import play.api.mvc.Results
import scala.concurrent.ExecutionContext
import play.api.Logger
import services.ClockLike
import play.api.libs.json.Json

/** UserExtractResult are all possible results for an user extraction from request.
  */
sealed trait UserExtractResult {
  val logMsg: String
}
case class SuccessUserExtractResult(user: User) extends UserExtractResult {
  val logMsg = f"Found user ${user.email}"
}
case class InactiveUserExtractResult(user: User) extends UserExtractResult {
  val logMsg = f"Found INACTIVE user ${user.email}"
}
case class MissingUserExtractResult(userId: Int) extends UserExtractResult {
  val logMsg = f"Could not find user with id ${userId}"
}
case class ExpiredTokenExtractResult(token: Token) extends UserExtractResult {
  val logMsg = f"Expired token ${token}"
}
case class InvalidTokenExtractResult() extends UserExtractResult {
  val logMsg = f"Invalid token value."
}
case class InvalidHeaderExtractResult() extends UserExtractResult {
  val logMsg = f"Invalid token value."
}
case class MissingHeaderExtractResult() extends UserExtractResult {
  val logMsg = f"Missing authentication header"
}

/** Service responsible for extracting an user from a request, validating the token.
  */
trait RequestUserExtractorLike extends Results {

  implicit val ec: ExecutionContext

  final def withUser(request: Request[AnyContent])(
      block: User => Future[Result]
  ): Future[Result] = {
    extractUser(request).flatMap {
      case SuccessUserExtractResult(u) =>
        block(u)
      case InactiveUserExtractResult(_) =>
        Future.successful(Forbidden(Json.obj("msg" -> "User is not active.")))
      case MissingUserExtractResult(_) | ExpiredTokenExtractResult(_) |
          InvalidTokenExtractResult() =>
        Future.successful(Unauthorized(Json.obj("msg" -> "Invalid token")))
      case InvalidHeaderExtractResult() | MissingHeaderExtractResult() =>
        Future.successful(
          Unauthorized(
            Json.obj("msg" -> "Invalid authentication header format")
          )
        )
    }
  }

  def extractUser(request: Request[AnyContent]): Future[UserExtractResult]

}

class RequestUserExtractor(
    userRepo: UserRepositoryLike,
    tokenRepo: AuthTokenRepositoryLike,
    clock: ClockLike
)(implicit
    val ec: ExecutionContext
) extends RequestUserExtractorLike {

  protected val logger = Logger(getClass())
  protected val TokenRegex = "^Bearer (.*)$".r

  override def extractUser(
      request: Request[AnyContent]
  ): Future[UserExtractResult] = {
    val result = request.headers.get("Authorization") match {
      case Some(header) =>
        header match {
          case TokenRegex(tokenValue) => {
            tokenRepo.read(tokenValue).flatMap {
              case Some(token) if token.isValid(clock.now()) =>
                userRepo.read(token.userId).map {
                  case Some(user) if user.isActive() =>
                    SuccessUserExtractResult(user)
                  case Some(user) if !user.isActive() =>
                    InactiveUserExtractResult(user)
                  case None => MissingUserExtractResult(token.userId)
                }
              case Some(token) =>
                Future.successful(ExpiredTokenExtractResult(token))
              case None => Future.successful(InvalidTokenExtractResult())
            }
          }
          case _ => Future.successful(InvalidHeaderExtractResult())
        }
      case None => Future.successful(MissingHeaderExtractResult())
    }
    result.foreach(r => logger.info(r.logMsg))
    result
  }
}

class FakeRequestUserExtractor(user: Option[User])(implicit
    val ec: ExecutionContext
) extends RequestUserExtractorLike {

  protected val logger = Logger(getClass())

  override def extractUser(
      request: Request[AnyContent]
  ): Future[UserExtractResult] =
    Future.successful(
      user
        .map(SuccessUserExtractResult.apply)
        .getOrElse(InvalidTokenExtractResult())
    )

}
