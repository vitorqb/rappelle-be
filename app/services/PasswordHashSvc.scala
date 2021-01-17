package services

import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

/** A service that knows how to hash password.
  */
trait PasswordHashSvcLike {
  def hash(input: String): String
  def unhash(input: String): Option[String]
}

class PasswordHashSvc(salt: String) extends PasswordHashSvcLike {

  override def hash(input: String): String = {
    val spec = new PBEKeySpec(input.toCharArray(), salt.getBytes(), 65536, 128)
    SecretKeyFactory
      .getInstance("PBKDF2WithHmacSHA1")
      .generateSecret(spec)
      .getEncoded()
      .map(_.toChar)
      .mkString
  }

  override def unhash(input: String): Option[String] = ???

}

class FakePasswordHashSvc extends PasswordHashSvcLike {

  override def hash(input: String): String = input + "_HASHED"
  override def unhash(input: String): Option[String] =
    if (input.endsWith("_HASHED"))
      Some(input.dropRight(7))
    else
      None

}
