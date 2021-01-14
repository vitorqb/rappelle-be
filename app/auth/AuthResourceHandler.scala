package auth

import com.google.inject.ImplementedBy
import scala.concurrent.Future
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

class AuthResourceHandler(
    authTokenRepo: AuthTokenRepositoryLike,
    userRepo: UserRepositoryLike,
    tokenGenerator: TokenGeneratorLike,
    emailConfirmationSvc: EmailConfirmationSvcLike,
    frontendUrl: String
)(implicit val ec: ExecutionContext)
    extends AuthResourceHandlerLike {

  def createToken(requestInput: CreateTokenRequestInput): Future[Token] = {
    userRepo.read(requestInput.email).flatMap {
      case None => throw new UserDoesNotExist()
      case Some(user) => {
        userRepo.passwordIsValid(user, requestInput.password).flatMap {
          case true =>
            authTokenRepo.create(
              CreateTokenRequest(
                user,
                tokenGenerator.genExpirationDate(),
                tokenGenerator.genValue()
              )
            )
          case false => throw new InvalidPassword
        }
      }
    }
  }

  def createUser(
      requestInput: CreateUserRequestInput
  )(implicit r: RequestHeader): Future[User] = {
    val request = CreateUserRequest(requestInput.email, requestInput.password)
    val callbackGenerator = new CallbackGeneratorLike {
      def render(key: String) = f"${frontendUrl}/#/emailConfirmation?key=$key"
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
