package auth

import com.google.inject.ImplementedBy
import scala.concurrent.Future
import com.google.inject.Inject
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[AuthResourceHandler])
trait AuthResourceHandlerLike {
  def createToken(request: CreateTokenRequestInput): Future[Token]
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
}
