package auth

import com.google.inject.Singleton
import com.google.inject.Inject
import play.api.mvc.ControllerComponents
import common.RappelleBaseController
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.Logger

@Singleton
class AuthController @Inject() (
    val controllerComponents: ControllerComponents,
    val resourceHandler: AuthResourceHandlerLike,
    val requestUserExtractor: RequestUserExtractorLike
)(implicit val ec: ExecutionContext)
    extends RappelleBaseController {

  import AuthJsonSerializers._
  val logger = Logger(getClass())

  def postToken = Action.async(parse.tolerantJson) { implicit request =>
    WithAuthErrorHandling {
      parseRequestJson[CreateTokenRequestInput] { createTokenRequest =>
        logger.info(f"CreateTokenRequestInput for ${createTokenRequest.email}")
        resourceHandler
          .createToken(createTokenRequest)
          .map(x => Ok(Json.toJson(x)))
      }
    }
  }

  def ping() = Action.async { implicit request =>
    WithAuthErrorHandling {
      requestUserExtractor.withUser(request) { user =>
        logger.info(f"Ping for ${user.email}")
        Future.successful(NoContent)
      }
    }
  }

  def postUser() = Action.async(parse.tolerantJson) { implicit request =>
    WithAuthErrorHandling {
      parseRequestJson[CreateUserRequestInput] { input =>
        logger.info(f"CreateUserRequestInput for ${input.email}")
        resourceHandler.createUser(input).map(x => Created(Json.toJson(x)))
      }
    }
  }

  def postEmailConfirmation() = Action.async(parse.tolerantJson) {
    implicit request =>
      WithAuthErrorHandling {
        parseRequestJson[EmailConfirmationRequest] { emailConfirmationRequest =>
          resourceHandler
            .confirmEmail(emailConfirmationRequest)
            .map {
              case SuccessEmailConfirmationResult(_) => {
                logger.info("Success email confirmation")
                NoContent: Result
              }
              case InvalidKeyEmailConfirmationResult() => {
                logger.info("Invalid key for email confirmation")
                BadRequest(Json.obj("msg" -> "The key is invalid"))
              }
              case ExpiredKeyEmailConfirmationResult() => {
                logger.info("Expired key for email confirmation")
                BadRequest(Json.obj("msg" -> "The key has expired"))
              }
            }
        }
      }
  }

}

object WithAuthErrorHandling extends Results {
  def apply(
      block: => Future[Result]
  )(implicit ec: ExecutionContext): Future[Result] = {
    Future(block).flatten.recover {
      case e: UserDoesNotExist =>
        BadRequest(Json.obj("msg" -> "User does not exist"))
      case e: UserAlreadyExists =>
        BadRequest(Json.obj("msg" -> "An user with this email already exists."))
      case e: InvalidPassword =>
        BadRequest(Json.obj("msg" -> "Invalid password!"))
    }
  }
}
