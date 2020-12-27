package services

import com.google.inject.AbstractModule
import com.google.inject.Provides
import play.api.Configuration
import play.api.db.Database
import org.joda.time.DateTime
import play.api.Logger

class ServiceModule extends AbstractModule {

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
}
