package meta

import com.google.inject.Inject
import play.api.Configuration
import play.api.Logger

/** Class used to perform any needed initialization.
  */
class ApplicationStart @Inject() (config: Configuration) {

  val logger = Logger(getClass())

  logger.info("Initializing application...")

  val Reg = "^.*(application\\..*?\\.conf).*$".r
  config.underlying.origin().description() match {
    case Reg(x) => logger.info(f"CONFIGURATION FILE: $x")
    case _      => logger.info("Unknown config file :(")
  }
}
