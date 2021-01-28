package auth

import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime

object AuthJsonSerializers {

  implicit val createTokenRequestInputReads: Reads[CreateTokenRequestInput] = (
    (JsPath \ "email").read[String](Reads.email) and
      (JsPath \ "password").read[String]
  )(CreateTokenRequestInput)

  implicit val createTokenRequestInputWrites: Writes[CreateTokenRequestInput] =
    (
      (JsPath \ "email").write[String] and
        (JsPath \ "password").write[String]
    )(unlift(CreateTokenRequestInput.unapply))

  implicit val tokenWrites: Writes[Token] = (
    (JsPath \ "value").write[String] and
      (JsPath \ "expiresAt").write[DateTime]
  )(token => (token.value, token.expiresAt))

  implicit val createUserRequestInputReads: Reads[CreateUserRequestInput] = (
    (JsPath \ "email").read[String](Reads.email) and
      (JsPath \ "password").read[String]
  )(CreateUserRequestInput)

  implicit val createUserRequestInputWrites: Writes[CreateUserRequestInput] = (
    (JsPath \ "email").write[String] and (JsPath \ "password").write[String]
  )(unlift(CreateUserRequestInput.unapply))

  implicit val userWrites: Writes[User] = (
    (JsPath \ "id").write[Int]
      and (JsPath \ "email").write[String]
      and (JsPath \ "isActive").write[Boolean]
  )((user: User) => (user.id, user.email, user.isActive()))

  implicit val createEmailConfirmationRequestWrites: Writes[EmailConfirmationRequest] = (
    (JsPath \ "key").write[String] and (JsPath \ "key").write[String]
  )(r => (r.key, r.key))

  implicit val createEmailConfirmationRequestReads: Reads[EmailConfirmationRequest] = (
    (JsPath \ "key").read[String] and (JsPath \ "key").read[String]
  )((key, _) => EmailConfirmationRequest(key))
}
