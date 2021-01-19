package services

import org.scalatestplus.play.PlaySpec
import functional.utils.WithTestApp

class EncryptionSvcFunSpec extends PlaySpec {

  "encrypt and descrypt" should {

    "encrypt and decrypt" in {
      WithTestContext() { c =>
        val enc = c.svc.encrypt("Hola soy un caracol")
        c.svc.decrypt(enc) must equal("Hola soy un caracol")
      }
    }

  }

  case class TestContext(svc: EncryptionSvcLike)

  object WithTestContext {
    def apply()(block: TestContext => Any) = {
      WithTestApp() { app =>
        block(TestContext(app.injector.instanceOf[EncryptionSvcLike]))
      }
    }
  }

}
