package auth

import play.api.mvc.Request
import scala.concurrent.Future

trait RequestTokenExtractorLike {
  import RequestTokenExtractorLike._
  def extractToken[A](request: Request[A]): Future[Result]
}

object RequestTokenExtractorLike {
  sealed trait Result
  case class MissingCookie() extends Result
  case class TokenNotFound() extends Result
  case class InvalidToken() extends Result
  case class Found(token: Token) extends Result
}
