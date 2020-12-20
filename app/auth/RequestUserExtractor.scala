package auth

import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Result
import scala.concurrent.Future
import play.api.mvc.Results
import scala.concurrent.ExecutionContext
import play.api.Logger

trait RequestUserExtractorLike extends Results {

  implicit val ec: ExecutionContext

  final def withUser(request: Request[AnyContent])(
      block: User => Future[Result]
  ): Future[Result] = {
    extractUser(request).flatMap {
      case None => Future.successful(Unauthorized)
      case Some(x) => block(x)
    }
  }

  def extractUser(request: Request[AnyContent]): Future[Option[User]]

}

class RequestUserExtractor(
    userRepo: UserRepositoryLike,
    tokenRepo: AuthTokenRepositoryLike
)(implicit
    val ec: ExecutionContext
) extends RequestUserExtractorLike {

  protected val logger = Logger(getClass())
  protected val TokenRegex = "^Bearer (.*)$".r

  override def extractUser(request: Request[AnyContent]): Future[Option[User]] =
    request.headers.get("Authorization") match {
      case Some(header) =>
        header match {
          case TokenRegex(tokenValue) => {
            tokenRepo.read(tokenValue).flatMap {
              case Some(token) =>
                userRepo.read(token.userId).map {
                  case Some(user) => {
                    logger.info(s"Found user: ${user}")
                    Some(user)
                  }
                  case None => {
                    logger.info(s"Could not find user with id ${token.userId}")
                    None
                  }
                }
              case None => {
                logger.info("Could not locate token for request")
                Future.successful(None)
              }
            }
          }
          case _ => {
            logger.info("Received invalid authentication header")
            Future.successful(None)
          }
        }
      case None => {
        logger.info("Received missing authentication header")
        Future.successful(None)
      }
    }
}

class FakeRequestUserExtractor(user: Option[User])(implicit
    val ec: ExecutionContext
) extends RequestUserExtractorLike {

  protected val logger = Logger(getClass())

  override def extractUser(request: Request[AnyContent]): Future[Option[User]] =
    Future.successful(user)

}
