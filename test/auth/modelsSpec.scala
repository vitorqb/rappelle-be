package auth

import org.scalatestplus.play.PlaySpec
import org.joda.time.DateTime

class TokenSpec extends PlaySpec {

  "isValid" should {
    "true right before expiration" in {
      val token = Token("a", DateTime.parse("2020-01-01T00:00:00"), 1)
      val now = token.expiresAt.minusMillis(1)
      token.isValid(now) must be(true)
    }
    "false right after expiration" in {
      val token = Token("a", DateTime.parse("2020-01-01T00:00:00"), 1)
      val now = token.expiresAt.plusMillis(1)
      token.isValid(now) must be(false)
    }
  }

}
