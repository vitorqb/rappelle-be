package services

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.Aead

trait EncryptionSvcLike {
  def encrypt(input: String): Array[Byte]
  def decrypt(input: Array[Byte]): String
}

class EncryptionSvc(keysetHandle: KeysetHandle) extends EncryptionSvcLike {

  override def encrypt(input: String): Array[Byte] =
    keysetHandle
      .getPrimitive(classOf[Aead])
      .encrypt(input.getBytes(), Array.emptyByteArray)

  override def decrypt(input: Array[Byte]): String =
    keysetHandle
      .getPrimitive(classOf[Aead])
      .decrypt(input, Array.emptyByteArray)
      .map(_.toChar)
      .mkString

}
