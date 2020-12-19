package auth

import play.api.mvc.BaseController
import com.google.inject.Singleton
import com.google.inject.Inject
import play.api.mvc.ControllerComponents

@Singleton
class AuthController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def postToken() = Action.async { implicit request =>
    ???
  }

}
