package reminders

import org.scalatestplus.play.PlaySpec
import functional.utils.WithAuthContext
import functional.utils.AuthContext

class RemindersFunSpec extends PlaySpec {

  "create reminder flow" should {
    "create and get a reminder for a given user" in {
      WithTestContext() { c =>

      }
    }
  }

  case class TestContext(authContext: AuthContext)

  object WithTestContext {
    def apply()(block: TestContext => Any): Any = {
      WithAuthContext() { authContext =>
        block(TestContext(authContext))
      }
    }
  }

}
