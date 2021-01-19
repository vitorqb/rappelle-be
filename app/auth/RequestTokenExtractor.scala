package auth

import play.api.mvc.Request
import scala.concurrent.Future
import com.google.inject.ImplementedBy
import scala.concurrent.ExecutionContext
import services.ClockLike
import services.PasswordHashSvcLike
import com.google.inject.Inject

@ImplementedBy(classOf[RequestTokenExtractor])
trait RequestTokenExtractorLike {
  import RequestTokenExtractorLike._
  def extractToken[A](request: Request[A]): Future[Result]
}

object RequestTokenExtractorLike {
  val COOKIE_NAME = "RappelleAuth"
  sealed trait Result
  case class MissingCookie() extends Result
  case class TokenNotFound() extends Result
  case class InvalidToken() extends Result
  case class Found(token: Token) extends Result
}

class RequestTokenExtractor @Inject() (
    tokenRepo: AuthTokenRepositoryLike,
    clock: ClockLike,
    passwordHashSvc: PasswordHashSvcLike
)(implicit val ec: ExecutionContext)
    extends RequestTokenExtractorLike {

  import RequestTokenExtractorLike._

  override def extractToken[A](request: Request[A]): Future[Result] = {
    request.cookies.get(COOKIE_NAME) match {
      case None => Future.successful(MissingCookie())
      case Some(cookie) =>
        passwordHashSvc.unhash(cookie.value) match {
          case None => Future.successful(TokenNotFound())
          case Some(tokenVal) =>
            tokenRepo.read(tokenVal).map {
              case None                                       => TokenNotFound()
              case Some(token) if !token.isValid(clock.now()) => InvalidToken()
              case Some(token) if token.isValid(clock.now())  => Found(token)
            }
        }
    }
  }

}
