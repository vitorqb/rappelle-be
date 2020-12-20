package auth

import com.google.inject.Singleton
import com.google.inject.Inject
import play.api.mvc.ControllerComponents
import common.RappelleBaseController
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json
import scala.concurrent.Future

@Singleton
class AuthController @Inject() (
    val controllerComponents: ControllerComponents,
    val resourceHandler: AuthResourceHandlerLike,
    val requestUserExtractor: RequestUserExtractorLike
)(implicit val ec: ExecutionContext)
    extends RappelleBaseController {

  import AuthJsonSerializers._

  def postToken = jsonAsyncAction { implicit request =>
    parseRequestJson[CreateTokenRequestInput] { createTokenRequest =>
      resourceHandler
        .createToken(createTokenRequest)
        .map(x => Ok(Json.toJson(x)))
    }
  }

  def ping() = Action.async { implicit request =>
    requestUserExtractor.withUser(request) { _ =>
      Future.successful(NoContent)
    }
  }

}
