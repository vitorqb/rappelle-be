package reminders

final case class ReminderNotFound(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)