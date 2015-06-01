package es.uvigo.ei.sing.sds
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity._
import util.Page

trait SearchTermsComponent {
  self: ArticlesComponent with KeywordsComponent with HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  class SearchTerms(tag: Tag) extends Table[SearchTerm](tag, "search_index") {
    def term         = column[String]("search_index_term")
    def tf           = column[Double]("search_index_tf")
    def idf          = column[Double]("search_index_idf")
    def tfidf        = column[Double]("search_index_tf_idf")
    def articleId    = column[Article.ID]("article_id")
    def keywordId    = column[Keyword.ID]("keyword_id")

    def pk = primaryKey("search_index_pk", (term, keywordId, articleId))

    def keyword    = foreignKey("search_index_keyword_fk", keywordId, keywords)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def article    = foreignKey("search_index_article_fk", articleId, articles)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

    def * = (term, tf, idf, tfidf, articleId, keywordId) <> (SearchTerm.tupled, SearchTerm.unapply)
  }

  val terms = TableQuery[SearchTerms]

}

final class SearchTermsDAO extends SearchTermsComponent with ArticlesComponent with KeywordsComponent with HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def count: Future[Int] =
    db.run(terms.length.result)

  def count(termFilter: String): Future[Int] =
    db.run {
      terms.filter(_.term.toLowerCase like termFilter.toLowerCase).length.result
    }

  def get(id: SearchTerm.ID): Future[Option[SearchTerm]] =
    db.run {
      terms.filter(term =>
        (term.articleId        === id._2) &&
        (term.keywordId        === id._3) &&
        (term.term.toLowerCase === id._1.toLowerCase)
      ).result.headOption
    }

  def list(page: Int = 0, pageSize: Int = 10, termFilter: String = "%"): Future[Page[SearchTerm]] = {
    val offset = pageSize * page

    val query = terms.filter(
      _.term.toLowerCase like termFilter.toLowerCase
    ).sortBy(_.tfidf.desc).drop(offset).take(pageSize)

    for {
      total  <- count(termFilter)
      result <- db.run(query.result)
    } yield Page(result, page, offset, total)
  }

  def insert(term: SearchTerm): Future[SearchTerm] =
    db.run((terms += term)).map(_ => term)

  def insert(terms: SearchTerm*): Future[Seq[SearchTerm]] =
    db.run((this.terms ++= terms)).map(_ => terms)

  def clear(): Future[Unit] =
    db.run(terms.delete).map(_ => ())

}
