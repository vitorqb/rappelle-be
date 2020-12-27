package auth

import com.google.inject.AbstractModule
import play.api.Configuration
import com.google.inject.Provides
import org.joda.time.DateTime
import play.api.db.Database
import scala.concurrent.ExecutionContext
import services.PasswordHashSvcLike
import services.UniqueIdGeneratorLike
import services.ClockLike

class AuthModule extends AbstractModule {

  @Provides
  def authTokenRepositoryLike(
      config: Configuration,
      db: Database,
      idGenerator: UniqueIdGeneratorLike
  )(implicit
      ec: ExecutionContext
  ): AuthTokenRepositoryLike = {
    config.getOptional[String]("auth.tokenRepository.type") match {
      case None | Some("AuthTokenRepository") =>
        new AuthTokenRepository(db, idGenerator)
      case Some("DummyAuthTokenRepository") => {
        val id = config.get[Int]("auth.fakeUser.id")
        val email = config.get[String]("auth.fakeUser.email")
        val password = config.get[String]("auth.fakeUser.password")
        val accessToken = config.get[String]("auth.fakeToken.value")
        val expiresAt = config.get[String]("auth.fakeToken.expiresAt")
        val user = User(id, email)
        val token = Token(accessToken, DateTime.parse(expiresAt), id)
        new DummyAuthTokenRepository(user, password, token)
      }
      case Some(x) =>
        throw new RuntimeException(
          s"Invalid configuration for auth.tokenRepository.type: ${x} "
        )
    }
  }

  @Provides
  def userRepositoryLike(
      db: Database,
      idGenerator: UniqueIdGeneratorLike,
      hashSvc: PasswordHashSvcLike,
      ec: ExecutionContext,
      config: Configuration
  ): UserRepositoryLike =
    config.getOptional[String]("auth.userRepository.type") match {
      case None | Some("UserRepository") =>
        new UserRepository(db, idGenerator, hashSvc)(ec)
      case Some("FakeUserRepository") => {
        val id = config.get[Int]("auth.fakeUser.id")
        val email = config.get[String]("auth.fakeUser.email")
        val password = config.get[String]("auth.fakeUser.password")
        val request = CreateUserRequest(email, password)
        val repo = new FakeUserRepository
        repo.create(request, id)
        repo
      }
      case _ =>
        throw new RuntimeException("Invalid value for auth.userRepository.type")
    }

  @Provides
  def tokenGeneratorLike(
      config: Configuration,
      clock: ClockLike
  ): TokenGeneratorLike =
    config.getOptional[String]("auth.tokenGenerator.type") match {
      case None | Some("TokenGenerator") =>
        new TokenGenerator(clock)
      case Some("FakeTokenGenerator") => {
        val value = config.get[String]("auth.fakeToken.value")
        val expiresAt =
          DateTime.parse(config.get[String]("auth.fakeToken.expiresAt"))
        new FakeTokenGenerator(value, expiresAt)
      }
      case _ =>
        throw new RuntimeException("Invalid value for auth.tokenGenerator.type")
    }

  @Provides
  def requestUserExtractorLike(
      userRepo: UserRepositoryLike,
      tokenRepo: AuthTokenRepositoryLike,
      clock: ClockLike,
      ec: ExecutionContext
  ): RequestUserExtractorLike =
    new RequestUserExtractor(userRepo, tokenRepo, clock)(ec)
}
