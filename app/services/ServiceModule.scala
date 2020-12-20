package services

import com.google.inject.AbstractModule
import com.google.inject.Provides
import play.api.Configuration
import play.api.db.Database

class ServiceModule extends AbstractModule {

  @Provides
  def uniqueIdGenerator(db: Database): UniqueIdGeneratorLike =
    new UniqueIdGenerator(db)

  @Provides
  def passwordHashSvc(config: Configuration): PasswordHashSvcLike =
    new PasswordHashSvc(config.get[String]("services.passwordHash.salt"))

  @Provides
  def clock(): ClockLike =
    new Clock()

}
