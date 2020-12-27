package services

import com.google.inject.AbstractModule
import com.google.inject.Provides
import play.api.Configuration
import play.api.db.Database
import org.joda.time.DateTime

class ServiceModule extends AbstractModule {

  @Provides
  def uniqueIdGenerator(db: Database): UniqueIdGeneratorLike =
    new UniqueIdGenerator(db)

  @Provides
  def passwordHashSvc(config: Configuration): PasswordHashSvcLike =
    new PasswordHashSvc(config.get[String]("services.passwordHash.salt"))

  @Provides
  def clock(config: Configuration): ClockLike =
    config.getOptional[String]("services.clock.type") match {
      case None | Some("Clock") =>
        new Clock()
      case Some("FakeClock") =>
        new FakeClock(DateTime.parse(config.get[String]("services.clock.now")))
      case Some(x) =>
        throw new RuntimeException(
          f"Invalid value for services.clock.type: ${x}"
        )
    }
}
