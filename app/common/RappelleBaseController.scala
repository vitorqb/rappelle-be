package common

import play.api.mvc.BaseController
import play.api.mvc.Request
import play.api.libs.json.JsValue
import play.api.mvc.Result
import scala.concurrent.Future
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.Reads
import play.api.mvc.Results

trait JsonRequestParser { self: Results =>

  def parseRequestJson[A](
      block: A => Future[Result]
  )(implicit req: Request[JsValue], rds: Reads[A]) = {
    req.body.validate[A] match {
      case JsSuccess(value, _) => block(value)
      case e: JsError          => Future.successful(BadRequest(JsError.toJson(e)))
    }
  }

}

trait JsonRequestAction { self: BaseController =>

  def jsonAsyncAction[A](block: Request[JsValue] => Future[Result]) =
    Action.async(parse.tolerantJson)(block)

}

abstract class RappelleBaseController
    extends BaseController
    with JsonRequestParser
    with JsonRequestAction
