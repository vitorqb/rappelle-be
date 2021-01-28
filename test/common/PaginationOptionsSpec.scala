package common

import org.scalatestplus.play.PlaySpec
import play.api.mvc.QueryStringBindable
import common.PaginationOptions._

class PaginationOptionsSpec extends PlaySpec {

  "queryStrinBindable" should {

    val bindable = implicitly[QueryStringBindable[PaginationOptions]]

    "create from query string" in {
      val qs = Map("page" -> Seq("2"), "itemsPerPage" -> Seq("20"))
      bindable.bind("", qs) must equal(Some(Right(PaginationOptions(2, 20))))
    }

    "create from query string with defaults" in {
      val qs = Map.empty[String, Seq[String]]
      bindable.bind("", qs) must equal(Some(Right(PaginationOptions(1, 30))))
    }

    "to query string" in {
      val opts = PaginationOptions(2, 20)
      bindable.unbind("", opts) must equal("page=2&itemsPerPage=20")
    }
  }
}
