package functional.utils

import play.api.Application
import play.api.db.Database
import play.api.db.evolutions.Evolutions
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import play.api.Logger
import com.typesafe.config.ConfigFactory

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
