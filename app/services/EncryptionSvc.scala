package services

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.config.TinkConfig

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

class FakeEncriptionSvc() extends EncryptionSvcLike {

  import FakeEncriptionSvc._

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

object FakeEncriptionSvc {

  TinkConfig.register();
  val keysetTemplate = AesGcmKeyManager.aes128GcmTemplate();
  val keysetHandle   = KeysetHandle.generateNew(keysetTemplate);

}
