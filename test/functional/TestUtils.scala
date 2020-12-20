package functional.utils

import play.api.Application
import play.api.db.Database
import play.api.db.evolutions.Evolutions
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import play.api.Logger

object WithTestApp {

  def getApp(conf: Map[String, Any]): Application =
    GuiceApplicationBuilder()
      .configure(conf)
      .configure(
        "db.default.url" -> "jdbc:postgresql://localhost:5000/rappelle-be-test"
      )
      .build()

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
