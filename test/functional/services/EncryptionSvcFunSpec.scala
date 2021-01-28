package services

import functional.utils.WithTestApp
import functional.utils.FunctionalSpec

class EncryptionSvcFunSpec extends FunctionalSpec {

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
