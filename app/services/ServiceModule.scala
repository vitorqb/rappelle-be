package services

import com.google.inject.AbstractModule
import com.google.inject.Provides
import play.api.Configuration
import play.api.db.Database
import org.joda.time.DateTime
import play.api.Logger
import play.api.ConfigLoader
import com.typesafe.config.Config
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext

class ServiceModule extends AbstractModule {

  import ServiceConfigLoaders._

  val logger = Logger(getClass())

  @Provides
  @com.google.inject.Singleton
  def uniqueIdGenerator(db: Database): UniqueIdGeneratorLike =
    new UniqueIdGenerator(db)

  @Provides
  @com.google.inject.Singleton
  def passwordHashSvc(config: Configuration): PasswordHashSvcLike =
    new PasswordHashSvc(config.get[String]("services.passwordHash.salt"))

  @Provides
  @com.google.inject.Singleton
  def clock(config: Configuration): ClockLike =
    config.getOptional[String]("services.clock.type") match {
      case None | Some("Clock") => {
        logger.info(f"Providing Clock")
        new Clock()
      }
      case Some("FakeClock") => {
        val now = DateTime.parse(config.get[String]("services.clock.now"))
        logger.info(f"Providing FakeClock($now)")
        new FakeClock(now)
      }
      case Some(x) =>
        throw new RuntimeException(
          f"Invalid value for services.clock.type: ${x}"
        )
    }

  @Provides
  @com.google.inject.Singleton
  def emailSvc(
      config: Configuration,
      ws: WSClient,
      ec: ExecutionContext
  ): EmailSvcLike =
    config.getOptional[String]("services.email.type") match {
      case None | Some("MailgunEmailSvc") => {
        logger.info(f"Providing MailgunEmailSvc")
        new MailgunEmailSvc(
          config.get[MailgunConfig]("services.email.mailgun"),
          ws
        )(ec)
      }
      case Some("FakeEmailService") => {
        logger.info(f"Providing FakeEmailService")
        new FakeEmailSvc
      }
      case Some(x) =>
        throw new RuntimeException(
          f"Invalid value for services.email.type: ${x}"
        )
    }
}

object ServiceConfigLoaders {
  implicit val mailgunConfigLoader: ConfigLoader[MailgunConfig] =
    new ConfigLoader[MailgunConfig] {

      override def load(config: Config, path: String): MailgunConfig = {
        val url = config.getString(f"${path}.url")
        val from = config.getString(f"${path}.from")
        val key = config.getString(f"${path}.key")
        MailgunConfig(url, from, key)
      }
    }
}
