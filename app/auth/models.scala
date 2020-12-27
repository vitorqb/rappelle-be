package auth
import org.joda.time.DateTime

case class CreateTokenRequestInput(val email: String, val password: String)
case class CreateTokenRequest(
    val user: User,
    val expiresAt: DateTime,
    val value: String
)
case class CreateUserRequest(val email: String, val password: String)
case class User(val id: Int, val email: String)
case class Token(val value: String, val expiresAt: DateTime, val userId: Int) {
  def isValid(now: DateTime): Boolean = expiresAt.isAfter(now)
}
