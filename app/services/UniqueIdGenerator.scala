package services
import anorm.SQL
import play.api.db.Database
import anorm.SqlParser

/** A service that knows how to generate unique ids.
  */
trait UniqueIdGeneratorLike {
  def gen(): Int
}

class UniqueIdGenerator(db: Database) extends UniqueIdGeneratorLike {

  val sequence = "uniqueIdSequence"

  override def gen(): Int = {
    db.withTransaction { implicit c =>
      SQL(f"SELECT nextval('${sequence}')").as(SqlParser.int("nextval").*).head
    }
  }

}

class FakeUniqueIdGenerator extends UniqueIdGeneratorLike {

  var id = 0

  override def gen(): Int = {
    id += 1
    id
  }

}
