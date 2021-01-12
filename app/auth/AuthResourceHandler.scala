package auth

import com.google.inject.ImplementedBy
import scala.concurrent.Future
import com.google.inject.Inject
import scala.concurrent.ExecutionContext
import play.api.mvc.RequestHeader

@ImplementedBy(classOf[AuthResourceHandler])
trait AuthResourceHandlerLike {
  def createToken(requestInput: CreateTokenRequestInput): Future[Token]
  def createUser(requestInput: CreateUserRequestInput)(implicit
      r: RequestHeader
  ): Future[User]
  def confirmEmail(
      request: EmailConfirmationRequest
  ): Future[EmailConfirmationResult]
}

class AuthResourceHandler @Inject() (
    authTokenRepo: AuthTokenRepositoryLike,
    userRepo: UserRepositoryLike,
    tokenGenerator: TokenGeneratorLike,
    emailConfirmationSvc: EmailConfirmationSvcLike
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

  def createUser(
      requestInput: CreateUserRequestInput
  )(implicit r: RequestHeader): Future[User] = {
    val request = CreateUserRequest(requestInput.email, requestInput.password)
    val callbackGenerator = new CallbackGeneratorLike {
      def render(key: String) =
        routes.AuthController
          .emailConfirmationCallback(key)
          .absoluteURL()
          .toString()
    }
    userRepo.read(request.email).flatMap {
      case None =>
        userRepo.create(request).flatMap { user =>
          emailConfirmationSvc.send(user, callbackGenerator).map { _ =>
            user
          }
        }
      case Some(_) => Future.failed(new UserAlreadyExists)
    }
  }

  def confirmEmail(
      request: EmailConfirmationRequest
  ): Future[EmailConfirmationResult] = {
    emailConfirmationSvc.confirm(request).flatMap { result =>
      result match {
        case SuccessEmailConfirmationResult(userId) => {
          val updateUserReq =
            UpdateUserRequest(userId, emailConfirmed = Some(true))
          userRepo.update(updateUserReq).map {
            case None    => throw new UserDoesNotExist
            case Some(_) => result
          }
        }
        case x => Future.successful(x)
      }
    }
  }
}
