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
import services.EmailSvcLike
import play.api.Logger

class AuthModule extends AbstractModule {

  val logger = Logger(getClass)

  @Provides
  def authResourceHandler(
      authTokenRepo: AuthTokenRepositoryLike,
      userRepo: UserRepositoryLike,
      tokenGenerator: TokenGeneratorLike,
      emailConfirmationSvc: EmailConfirmationSvcLike,
      ec: ExecutionContext,
      config: Configuration
  ) = {
    new AuthResourceHandler(
      authTokenRepo,
      userRepo,
      tokenGenerator,
      emailConfirmationSvc,
      config.get[String]("frontendUrl")
    )(ec)
  }

  @Provides
  @com.google.inject.Singleton
  def authTokenRepositoryLike(
      config: Configuration,
      db: Database,
      idGenerator: UniqueIdGeneratorLike
  )(implicit
      ec: ExecutionContext
  ): AuthTokenRepositoryLike = {
    config.getOptional[String]("auth.tokenRepository.type") match {
      case None | Some("AuthTokenRepository") => {
        logger.info("Providing AuthTokenRepository")
        new AuthTokenRepository(db, idGenerator)
      }
      case Some("DummyAuthTokenRepository") => {
        val id = config.get[Int]("auth.fakeUser.id")
        val email = config.get[String]("auth.fakeUser.email")
        val password = config.get[String]("auth.fakeUser.password")
        val emailConfirmed = config.get[Boolean]("auth.fakeUser.emailConfirmed")
        val accessToken = config.get[String]("auth.fakeToken.value")
        val expiresAt = config.get[String]("auth.fakeToken.expiresAt")
        val user = User(id, email, emailConfirmed)
        val token = Token(accessToken, DateTime.parse(expiresAt), id)
        logger.info(
          f"Providing with DummyAuthTokenRepository($user, $password, $token)"
        )
        new DummyAuthTokenRepository(user, password, token)
      }
      case Some(x) =>
        throw new RuntimeException(
          s"Invalid configuration for auth.tokenRepository.type: ${x} "
        )
    }
  }

  @Provides
  @com.google.inject.Singleton
  def userRepositoryLike(
      db: Database,
      idGenerator: UniqueIdGeneratorLike,
      hashSvc: PasswordHashSvcLike,
      ec: ExecutionContext,
      config: Configuration
  ): UserRepositoryLike =
    config.getOptional[String]("auth.userRepository.type") match {
      case None | Some("UserRepository") => {
        logger.info(f"Providing UserRepository")
        new UserRepository(db, idGenerator, hashSvc)(ec)
      }
      case Some("FakeUserRepository") => {
        val id = config.get[Int]("auth.fakeUser.id")
        val email = config.get[String]("auth.fakeUser.email")
        val password = config.get[String]("auth.fakeUser.password")
        val emailConfirmed = config.get[Boolean]("auth.fakeUser.emailConfirmed")
        val request = CreateUserRequest(email, password)
        val repo = new FakeUserRepository
        repo.create(request, id, emailConfirmed)
        logger.info(
          f"Providing FakeUserRepository.create($request, $id, $emailConfirmed)"
        )
        repo
      }
      case _ =>
        throw new RuntimeException("Invalid value for auth.userRepository.type")
    }

  @Provides
  @com.google.inject.Singleton
  def tokenGeneratorLike(
      config: Configuration,
      clock: ClockLike
  ): TokenGeneratorLike =
    config.getOptional[String]("auth.tokenGenerator.type") match {
      case None | Some("TokenGenerator") => {
        logger.info("Providing TokenGenerator")
        new TokenGenerator(clock)
      }
      case Some("FakeTokenGenerator") => {
        val value = config.get[String]("auth.fakeToken.value")
        val expiresAt =
          DateTime.parse(config.get[String]("auth.fakeToken.expiresAt"))
        logger.info(f"Providing with FakeTokenGenerator($value, $expiresAt)")
        new FakeTokenGenerator(value, expiresAt)
      }
      case _ =>
        throw new RuntimeException("Invalid value for auth.tokenGenerator.type")
    }

  @Provides
  @com.google.inject.Singleton
  def requestUserExtractorLike(
      userRepo: UserRepositoryLike,
      tokenRepo: AuthTokenRepositoryLike,
      clock: ClockLike,
      ec: ExecutionContext
  ): RequestUserExtractorLike =
    new RequestUserExtractor(userRepo, tokenRepo, clock)(ec)

  @Provides
  @com.google.inject.Singleton
  def emailConfirmationSvcLike(
      config: Configuration,
      repo: EmailConfirmationRepositoryLike,
      clock: ClockLike,
      ec: ExecutionContext,
      keyGenerator: TokenGeneratorLike,
      emailSvc: EmailSvcLike
  ): EmailConfirmationSvcLike =
    config.getOptional[String]("auth.emailConfirmationSvc.type") match {
      case None | Some("EmailConfirmationSvc") => {
        val expirationSeconds =
          config.get[Int]("auth.emailConfirmationSvc.expirationSeconds")
        logger.info(
          f"Providing EmailConfirmationSvc with expirationSeconds=$expirationSeconds"
        )
        new EmailConfirmationSvc(
          repo,
          clock,
          expirationSeconds,
          keyGenerator,
          emailSvc
        )(ec)
      }
      case Some("FakeEmailConfirmationSvc") => {
        val confirmationKey =
          config.get[String]("auth.fakeEmailConfirmationSvc.confirmationKey")
        val userId =
          config.get[Int]("auth.fakeEmailConfirmationSvc.userId")
        logger.info(f"Providing FakeEmailConfirmationSvc($confirmationKey)")
        new FakeEmailConfirmationSvc(confirmationKey, userId)
      }
      case x =>
        throw new RuntimeException(
          f"Invalid value $x for auth.emailConfirmationSvc.type"
        )
    }

  @Provides
  @com.google.inject.Singleton
  def emailConfirmationRepositoryLike(
      config: Configuration,
      db: Database,
      idGenerator: UniqueIdGeneratorLike,
      ec: ExecutionContext
  ): EmailConfirmationRepositoryLike =
    config.getOptional[String]("auth.emailConfirmationRepository.type") match {
      case None | Some("EmailConfirmationRepository") => {
        logger.info(f"Providing EmailConfirmationRepository")
        new EmailConfirmationRepository(db, idGenerator)(ec)
      }
      case Some("FakeEmailConfirmationRepository") => {
        logger.info(f"Providing FakeEmailConfirmationRepository()")
        new FakeEmailConfirmationRepository
      }
      case x =>
        throw new RuntimeException(
          f"Invalid value $x for auth.emailConfirmationRepository.type"
        )
    }
}
