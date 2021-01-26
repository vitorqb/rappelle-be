package reminders

import com.google.inject.Inject
import play.api.mvc.ControllerComponents
import auth.RequestUserExtractorLike
import scala.concurrent.ExecutionContext
import play.api.Logger
import common.RappelleBaseController

@com.google.inject.Singleton
class RemindersController @Inject() (
    val controllerComponents: ControllerComponents,
    resourceHandler: RemindersResourceHandlerLike,
    requestUserExtractor: RequestUserExtractorLike
)(implicit val ec: ExecutionContext)
    extends RappelleBaseController {

  val logger = Logger(getClass())

  def listReminders = Action.async { implicit request =>
    ???
  }

  def postReminder = Action.async(parse.tolerantJson) { implicit request =>
    ???
  }

}
