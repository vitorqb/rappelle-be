package auth
import org.joda.time.DateTime

/** Token models
  */
case class CreateTokenRequestInput(val email: String, val password: String)
case class CreateTokenRequest(
    val user: User,
    val expiresAt: DateTime,
    val value: String
)
case class Token(val value: String, val expiresAt: DateTime, val userId: Int) {
  def isValid(now: DateTime): Boolean = expiresAt.isAfter(now)
}

/** User models
  */
case class CreateUserRequestInput(val email: String, val password: String)
case class CreateUserRequest(
    val email: String,
    val password: String
)
case class UpdateUserRequest(
    userId: Int,
    emailConfirmed: Option[Boolean] = None
)
case class User(val id: Int, val email: String, val emailConfirmed: Boolean) {
  def isActive(): Boolean = emailConfirmed
}

/** Email confirmation
  */
case class EmailConfirmationRequest(key: String)
case class EmailConfirmation(
    id: Int,
    userId: Int,
    key: String,
    sentAt: DateTime,
    responseReceivedAt: Option[DateTime]
) {
  def isValid(now: DateTime, expirationSeconds: Int): Boolean =
    now.isBefore(sentAt.plusSeconds(expirationSeconds))
}
case class CreateEmailConfirmationRequest(
    userId: Int,
    key: String,
    sentAt: DateTime
)
case class UpdateEmailConfirmationRequest(
    id: Int,
    responseReceivedAt: Option[Option[DateTime]] = None
)

/** Basic trait representing the possible outcomes for creating an email confirmation.
  */
sealed trait EmailConfirmationResult {}
case class SuccessEmailConfirmationResult(userId: Int) extends EmailConfirmationResult
case class ExpiredKeyEmailConfirmationResult()         extends EmailConfirmationResult
case class InvalidKeyEmailConfirmationResult()         extends EmailConfirmationResult
