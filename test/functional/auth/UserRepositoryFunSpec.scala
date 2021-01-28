package auth

import functional.utils.WithTestApp
import functional.utils.WithTestDb
import org.scalatest.concurrent.ScalaFutures
import services.FakeUniqueIdGenerator
import services.FakePasswordHashSvc
import functional.utils.FunctionalSpec

class UserRepositoryFunSpec extends FunctionalSpec with ScalaFutures {

  "create and read an user with email" in {
    WithTestContext() { c =>
      c.repo.create(c.request).futureValue
      val result = c.repo.read(c.request.email).futureValue.get
      result must equal(User(1, c.request.email, emailConfirmed = false))
    }
  }

  "create and read an user with id" in {
    WithTestContext() { c =>
      c.repo.create(c.request).futureValue
      val expectedId = c.idGenerator.lastVal()
      val result     = c.repo.read(expectedId).futureValue.get
      result must equal(
        User(expectedId, c.request.email, emailConfirmed = false)
      )
    }
  }

  "create and update an user" in {
    WithTestContext() { c =>
      val createdUser = c.repo.create(c.request).futureValue
      val updateRequest =
        UpdateUserRequest(createdUser.id, emailConfirmed = Some(true))
      val updatedUser = c.repo.update(updateRequest).futureValue
      val expected    = createdUser.copy(emailConfirmed = true)
      updatedUser must equal(Some(expected))
      c.repo.read(updateRequest.userId).futureValue must equal(Some(expected))
    }
  }

  "create and update without any change" in {
    WithTestContext() { c =>
      val createdUser   = c.repo.create(c.request).futureValue
      val updateRequest = UpdateUserRequest(createdUser.id)
      val updatedUser   = c.repo.update(updateRequest).futureValue
      updatedUser must equal(Some(createdUser))
      c.repo.read(updateRequest.userId).futureValue must equal(
        Some(createdUser)
      )
    }
  }

  "update an user that does not exist" in {
    WithTestContext() { c =>
      c.repo.update(UpdateUserRequest(1)).futureValue must equal(None)
    }
  }
  "return None if no user" in {
    WithTestContext() { c =>
      c.repo.read("foo").futureValue must equal(None)
    }
  }

  "validate user password" in {
    WithTestContext() { c =>
      val user = c.repo.create(c.request).futureValue
      c.repo.passwordIsValid(user, "foo").futureValue must equal(false)
      c.repo.passwordIsValid(user, c.request.password).futureValue must equal(
        true
      )
    }
  }

  case class TestContext(
      request: CreateUserRequest,
      idGenerator: FakeUniqueIdGenerator,
      repo: UserRepositoryLike
  )

  object WithTestContext {
    val request = CreateUserRequest("a@b.c", "password")
    def apply()(block: TestContext => Any): Any = {
      WithTestApp() { app =>
        WithTestDb(app) { db =>
          val idGenerator = new FakeUniqueIdGenerator
          val hashSvc     = new FakePasswordHashSvc
          block(
            TestContext(
              request,
              idGenerator,
              repo = new UserRepository(db, idGenerator, hashSvc)
            )
          )
        }
      }
    }
  }
}
