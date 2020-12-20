package auth

final case class UserDoesNotExist(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)
