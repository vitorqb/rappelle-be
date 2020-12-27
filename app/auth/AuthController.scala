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
        logger.info(f"Ping for ${user}")
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
}

object WithAuthErrorHandling extends Results {
  def apply(
      block: => Future[Result]
  )(implicit ec: ExecutionContext): Future[Result] = {
    Future(block).flatten.recover { case e: UserDoesNotExist =>
      BadRequest(Json.obj("msg" -> "User does not exist"))
    }
  }
}
