package auth

import scala.util.Random
import org.joda.time.DateTime
import services.ClockLike

/** Helper trait to generate tokens.
  */
trait TokenGeneratorLike {
  def genValue(): String
  def genValue(length: Int): String
  def genExpirationDate(): DateTime
}

class TokenGenerator(clock: ClockLike) extends TokenGeneratorLike {
  val length = 120
  val chars = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
  val tokenDurationSeconds = 30 * 24 * 60 * 60
  override def genValue(): String = genValue(length)
  override def genValue(length: Int): String =
    Random.shuffle(chars).take(length).mkString
  override def genExpirationDate(): DateTime =
    clock.now().plusSeconds(tokenDurationSeconds)
}

class FakeTokenGenerator(token: String, expirationDate: DateTime)
    extends TokenGeneratorLike {
  override def genExpirationDate(): DateTime = expirationDate
  override def genValue(): String = token
  override def genValue(length: Int): String = ???
}
