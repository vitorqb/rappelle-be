package common

import play.api.mvc.QueryStringBindable

case class PaginationOptions(page: Int, itemsPerPage: Int)

object PaginationOptions {
  implicit def queryStringBindable(implicit intBinder: QueryStringBindable[Int]) =
    new QueryStringBindable[PaginationOptions] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]
      ): Option[Either[String, PaginationOptions]] = {
        val page         = intBinder.bind("page", params).flatMap(_.toOption).getOrElse(1)
        val itemsPerPage = intBinder.bind("itemsPerPage", params).flatMap(_.toOption).getOrElse(30)
        Some(Right(PaginationOptions(page, itemsPerPage)))
      }
      override def unbind(key: String, paginationOpts: PaginationOptions): String = {
        intBinder.unbind("page", paginationOpts.page) + "&" + intBinder.unbind(
          "itemsPerPage",
          paginationOpts.itemsPerPage
        )
      }
    }
}
