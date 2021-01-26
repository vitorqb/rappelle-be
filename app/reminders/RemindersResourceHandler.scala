package reminders

import com.google.inject.ImplementedBy
import com.google.inject.Inject

@ImplementedBy(classOf[RemindersResourceHandler])
trait RemindersResourceHandlerLike {}

class RemindersResourceHandler @Inject() () extends RemindersResourceHandlerLike
