package functional.utils

import play.api.Application
import play.api.db.Database
import play.api.db.evolutions.Evolutions
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import play.api.Logger
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.WSClient
import play.api.test.TestServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatest.time.Seconds
import org.scalatest.time.Millis
import org.scalatest.time.Span
import scala.concurrent.ExecutionContext

object WithTestApp {

  def getApp(conf: Map[String, Any]): Application = {
    val funTestConfig =
      ConfigFactory.parseResources("application.funTest.conf").resolve()
    GuiceApplicationBuilder()
      .configure(conf)
      .configure(
        "db.default.driver" -> funTestConfig.getString("funtest.db.driver"),
        "db.default.url" -> funTestConfig.getString("funtest.db.url"),
        "db.default.username" -> funTestConfig.getString("funtest.db.username"),
        "db.default.password" -> funTestConfig.getString("funtest.db.password")
      )
      .build()
  }

  def apply(conf: Map[String, Any])(block: Application => Any): Any = {
    val app = getApp(conf)
    Helpers.running(app) {
      block(app)
    }
  }

  def apply()(block: Application => Any): Any = apply(Map())(block)

}

/** Provides a database for tests.
  * Can be used like this:
  *   WithTestDb(app) { db =>
  *     ...
  *   }
  */
object WithTestDb {

  val logger = Logger(getClass())

  def apply(app: Application)(block: Database => Any) = {
    val db = app.injector.instanceOf[Database]
    logger.info(f"Using db: ${db.url}")
    try {
      Evolutions.applyEvolutions(db)
      block(db)
    } finally {
      Evolutions.cleanupEvolutions(db)
      db.shutdown()
    }
  }
}

object TestUtils {
  val testServerPort = 10301
  val testServerUrl = s"http://localhost:${testServerPort}"
}

/** Provides a test context with an ulogged user
  */
case class AuthContext(
    app: Application,
    email: String,
    password: String,
    token: String,
    expiresAt: String,
    confirmationKey: String,
    id: Int
) {

  lazy val wsclient = app.injector.instanceOf[WSClient]

  def request(url: String) = wsclient.url(s"${TestUtils.testServerUrl}${url}")

  def requestWithToken(url: String) =
    request(url).withHttpHeaders("Authorization" -> s"Bearer $token")
}

object WithAuthContext {

  lazy val id = 123
  lazy val email = "a@b.c"
  lazy val password = "abc"
  lazy val token = "TOKEN"
  lazy val expiresAt = "2020-10-11T00:00:00.000Z"
  lazy val now = "2020-01-11T00:00:00.000Z"
  lazy val confirmationKey = "confirmationkey"
  lazy val appConf = Map(
    "auth.fakeUser.id" -> id,
    "auth.fakeUser.email" -> email,
    "auth.fakeUser.password" -> password,
    "auth.fakeUser.emailConfirmed" -> true,
    "auth.fakeToken.value" -> token,
    "auth.fakeToken.expiresAt" -> expiresAt,
    "auth.fakeEmailConfirmationSvc.confirmationKey" -> confirmationKey,
    "auth.fakeEmailConfirmationSvc.userId" -> id,
    "services.clock.now" -> now
  )

  def apply()(block: AuthContext => Any): Any = apply(identity)(block)

  def apply(
      configFn: Map[String, Any] => Map[String, Any]
  )(
      block: AuthContext => Any
  ): Any = {
    WithTestApp(configFn(appConf)) { app =>
      Helpers.running(TestServer(TestUtils.testServerPort, app)) {
        WithTestDb(app) { _ =>
          val context =
            AuthContext(
              app,
              email,
              password,
              token,
              expiresAt,
              confirmationKey,
              id
            )
          block(context)
        }
      }
    }
  }
}

trait FunctionalSpec extends PlaySpec with ScalaFutures {

  implicit val ec = ExecutionContext.global

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

}
