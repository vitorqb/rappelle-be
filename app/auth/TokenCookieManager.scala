package auth

import play.api.mvc.Request
import scala.concurrent.Future
import com.google.inject.ImplementedBy
import scala.concurrent.ExecutionContext
import services.ClockLike
import com.google.inject.Inject
import services.EncryptionSvcLike
import scala.util.Success
import scala.util.Try
import scala.util.Failure
import play.api.Logger
import java.util.Base64
import play.api.mvc.Cookie
import play.api.mvc.Cookie.SameSite

@ImplementedBy(classOf[TokenCookieManager])
trait TokenCookieManagerLike {
  import TokenCookieManagerLike._
  def extractToken[A](request: Request[A]): Future[Result]
  def cookie(token: Token): Cookie
}

object TokenCookieManagerLike {
  val COOKIE_NAME = "RappelleAuth"
  sealed trait Result
  case class MissingCookie() extends Result
  case class TokenNotFound() extends Result
  case class InvalidToken() extends Result
  case class Found(token: Token) extends Result
}

class TokenCookieManager @Inject() (
    tokenRepo: AuthTokenRepositoryLike,
    clock: ClockLike,
    encryptionSvc: EncryptionSvcLike
)(implicit val ec: ExecutionContext)
    extends TokenCookieManagerLike {

  import TokenCookieManagerLike._
  val logger = Logger(getClass())

  override def extractToken[A](request: Request[A]): Future[Result] = {
    request.cookies.get(COOKIE_NAME) match {
      case None => Future.successful(MissingCookie())
      case Some(cookie) => {
        Try {
          encryptionSvc.decrypt(Base64.getDecoder().decode(cookie.value))
        } match {
          case Failure(e: Throwable) => Future.successful(TokenNotFound())
          case Success(tokenVal) =>
            tokenRepo.read(tokenVal).map {
              case None                                       => TokenNotFound()
              case Some(token) if !token.isValid(clock.now()) => InvalidToken()
              case Some(token) if token.isValid(clock.now())  => Found(token)
            }
        }
      }
    }
  }

  override def cookie(token: Token): Cookie = {
    Cookie(
      COOKIE_NAME,
      Base64.getEncoder().encodeToString(encryptionSvc.encrypt(token.value)),
      httpOnly = true,
      sameSite = Some(SameSite.Strict)
    )
  }

}
