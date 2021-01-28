package services

import functional.utils.WithTestApp
import functional.utils.WithTestDb
import functional.utils.FunctionalSpec

class UniqueIdGeneratorFunSpec extends FunctionalSpec {

  "generate a sequence of unique ids" in {
    WithTestApp() { app =>
      WithTestDb(app) { db =>
        val generator = new UniqueIdGenerator(db)
        generator.gen() mustEqual 1
        generator.gen() mustEqual 2
        generator.gen() mustEqual 3
      }
    }
  }

}
