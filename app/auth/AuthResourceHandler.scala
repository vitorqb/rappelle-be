package auth

import com.google.inject.ImplementedBy
import scala.concurrent.Future
import com.google.inject.Inject
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[AuthResourceHandler])
trait AuthResourceHandlerLike {
  def createToken(requestInput: CreateTokenRequestInput): Future[Token]
  def createUser(requestInput: CreateUserRequestInput): Future[User]
}

class AuthResourceHandler @Inject() (
    authTokenRepo: AuthTokenRepositoryLike,
    userRepo: UserRepositoryLike,
    tokenGenerator: TokenGeneratorLike
)(implicit val ec: ExecutionContext)
    extends AuthResourceHandlerLike {

  def createToken(requestInput: CreateTokenRequestInput): Future[Token] = {
    userRepo.read(requestInput.email).flatMap {
      case None => throw new UserDoesNotExist()
      case Some(user) => {
        val request =
          CreateTokenRequest(
            user,
            tokenGenerator.genExpirationDate(),
            tokenGenerator.genValue()
          )
        authTokenRepo.create(request)
      }
    }
  }

  def createUser(requestInput: CreateUserRequestInput): Future[User] = {
    val request = CreateUserRequest(requestInput.email, requestInput.password)
    userRepo.read(request.email).flatMap {
      case None => userRepo.create(request)
      case Some(_) => Future.failed(new UserAlreadyExists)
    }
  }
}
