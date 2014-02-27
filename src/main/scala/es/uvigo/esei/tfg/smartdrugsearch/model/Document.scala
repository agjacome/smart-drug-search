package es.uvigo.esei.tfg.smartdrugsearch.model

import org.virtuslab.unicorn.ids._
import scala.slick.session.Session

final case class DocumentId (val id : Long) extends AnyVal with BaseId

object DocumentId extends IdCompanion[DocumentId] {

  object Predef {
    import scala.language.implicitConversions

    implicit def longToDocumentId(id : Long) : DocumentId = DocumentId(id)
    implicit def documentIdToLong(id : DocumentId) : Long = id.id
  }

}

case class Document (
  val id    : Option[DocumentId],
  val title : Sentence,
  val text  : String
) extends WithId[DocumentId] {

  require(!text.isEmpty, "Document text must be non-empty")

}

object Documents extends IdTable[DocumentId, Document]("documents") {

  import Sentence.Predef._

  def title = column[Sentence]("title", O.NotNull)
  def text  = column[String]("abstract", O.NotNull, O.DBType("TEXT"))

  private def base = title ~ text

  override def * = id.? ~: base <> (Document.apply _, Document.unapply _)

  override def insertOne(doc : Document)(implicit session : Session) : DocumentId =
    saveBase(base, Document.unapply _)(doc)

}

