package auth

import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime

object AuthJsonSerializers {

  implicit val createTokenRequestReads: Reads[CreateTokenRequestInput] = (
    (JsPath \ "email").read[String](Reads.email) and
      (JsPath \ "password").read[String]
  )(CreateTokenRequestInput)

  implicit val createTokenRequestWrites: Writes[CreateTokenRequestInput] = (
    (JsPath \ "email").write[String] and
      (JsPath \ "password").write[String]
  )(unlift(CreateTokenRequestInput.unapply))

  implicit val tokenWrites: Writes[Token] = (
    (JsPath \ "value").write[String] and
      (JsPath \ "expiresAt").write[DateTime]
  )(token => (token.value, token.expiresAt))

}
