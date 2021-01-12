package services

import scala.concurrent.Future
import play.api.Logger
import play.api.libs.ws.{WSAuthScheme, WSClient}
import scala.concurrent.ExecutionContext
import scala.collection.mutable.ListBuffer

trait EmailSvcLike {
  def send(to: String, subject: String, content: String): Future[Unit]
}

//Mailgun impl
case class MailgunConfig(url: String, from: String, key: String)

class MailgunEmailSvc(config: MailgunConfig, ws: WSClient)(implicit
    ec: ExecutionContext
) extends EmailSvcLike {

  val logger = Logger(getClass())

  def send(to: String, subject: String, content: String): Future[Unit] = {
    logger.info(f"Sending email to $to with subject $subject")
    ws.url(config.url)
      .withAuth("api", config.key, WSAuthScheme.BASIC)
      .post(
        Map(
          "from" -> config.from,
          "to" -> to,
          "subject" -> subject,
          "text" -> content
        )
      )
      .map(r => {
        logger.info(
          f"Received response from mailgun with ${r.status} and ${r.body}"
        )
      })
      .map(_ => ())
  }
}

//Fake Impl
class FakeEmailSvc extends EmailSvcLike {
  def send(to: String, subject: String, content: String): Future[Unit] = ???
}

//Test Impl
class TestEmailSvc extends EmailSvcLike {

  import TestEmailSvc._

  private var sentEmails = Seq.empty[SentEmail]
  private val logger = Logger(getClass())

  def getSentEmails() = sentEmails

  override def send(
      to: String,
      subject: String,
      content: String
  ): Future[Unit] = {
    val sent = SentEmail(to, subject, content)
    logger.info(f"Sending email: $sent")
    sentEmails = sentEmails ++ Seq(sent)
    Future.successful((): Unit)
  }

}

object TestEmailSvc {

  case class SentEmail(to: String, subject: String, content: String)

}
