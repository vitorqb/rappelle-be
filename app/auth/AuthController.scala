package auth

import com.google.inject.Singleton
import com.google.inject.Inject
import play.api.mvc.ControllerComponents
import common.RappelleBaseController
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json
import scala.concurrent.Future
import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results

@Singleton
class AuthController @Inject() (
    val controllerComponents: ControllerComponents,
    val resourceHandler: AuthResourceHandlerLike,
    val requestUserExtractor: RequestUserExtractorLike
)(implicit val ec: ExecutionContext)
    extends RappelleBaseController {

  import AuthJsonSerializers._

  def postToken = AuthErrorHandlingAction {
    Action.async(parse.tolerantJson) { implicit request =>
      parseRequestJson[CreateTokenRequestInput] { createTokenRequest =>
        resourceHandler
          .createToken(createTokenRequest)
          .map(x => Ok(Json.toJson(x)))
      }
    }
  }

  def ping() = AuthErrorHandlingAction {
    Action.async { implicit request =>
      requestUserExtractor.withUser(request) { _ =>
        Future.successful(NoContent)
      }
    }
  }

}

case class AuthErrorHandlingAction[A](action: Action[A])
    extends Action[A]
    with Results {
  implicit val ec = action.executionContext

  protected val handleError: PartialFunction[Throwable, Result] = {
    case e: UserDoesNotExist =>
      BadRequest(Json.obj("msg" -> "User does not exist"))
  }

  override def apply(request: Request[A]): Future[Result] =
    Future(action(request)).flatten.recover(handleError)

  override def parser = action.parser
  override def executionContext = action.executionContext
}
