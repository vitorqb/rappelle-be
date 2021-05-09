package reminders

import com.google.inject.Inject
import play.api.mvc.ControllerComponents
import auth.RequestUserExtractorLike
import scala.concurrent.ExecutionContext
import play.api.Logger
import common.RappelleBaseController
import auth.WithAuthErrorHandling
import play.api.libs.json.Json
import ReminderJsonSerializers._
import common.PaginationOptions

@com.google.inject.Singleton
class RemindersController @Inject() (
    val controllerComponents: ControllerComponents,
    resourceHandler: RemindersResourceHandlerLike,
    requestUserExtractor: RequestUserExtractorLike
)(implicit val ec: ExecutionContext)
    extends RappelleBaseController {

  val logger = Logger(getClass())

  def listReminders(pagOpts: PaginationOptions) = Action.async { implicit request =>
    WithAuthErrorHandling {
      requestUserExtractor.withUser(request) { user =>
        val req = ListReminderRequest(user, pagOpts.itemsPerPage, pagOpts.page)
        resourceHandler.listReminders(req).map(x => Ok(Json.toJson(x)))
      }
    }
  }

  def postReminder = Action.async(parse.tolerantJson) { implicit request =>
    WithAuthErrorHandling {
      requestUserExtractor.withUser(request) { user =>
        parseRequestJson[CreateReminderRequestInput] { input =>
          logger.info(s"Handling postReminder for user ${user.email}")
          resourceHandler.createReminder(
            CreateReminderRequest(
              user = user,
              title = input.title,
              datetime = input.datetime
            )
          ) map { reminder =>
            Ok(Json.toJson(reminder))
          }
        }
      }
    }
  }

  def deleteReminder(id: Int) = Action.async { implicit request =>
    WithAuthErrorHandling {
      requestUserExtractor.withUser(request) { user =>
        val req = DeleteReminderRequest(id, user)
        resourceHandler
          .deleteReminder(req)
          .map { _ =>
            Ok
          }
          .recover { case _: ReminderNotFound =>
            NotFound
          }
      }
    }
  }

}
