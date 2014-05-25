package es.uvigo.esei.tfg.smartdrugsearch.entity

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class AnnotationId (value : Long) extends AnyVal with Identifier
object AnnotationId extends IdentifierCompanion[AnnotationId]

final case class Annotation (
  id            : Option[AnnotationId] = None,
  documentId    : DocumentId,
  keywordId     : KeywordId,
  text          : Sentence,
  startPosition : Position,
  endPosition   : Position
) {

  require(startPosition < endPosition, "Start Position must be inferior than End Position")

}

object Annotation extends ((Option[AnnotationId], DocumentId, KeywordId, Sentence, Position, Position) => Annotation) {

  implicit val annotationFormat = Json.format[Annotation]

}

final case class AnnotationList (
  totalCount : Size,
  pageNumber : Position,
  pageSize   : Size,
  list       : Seq[Annotation]
) extends EntityList[Annotation]

object AnnotationList extends ((Size, Position, Size, Seq[Annotation]) => AnnotationList) {

  implicit val annotationListWrites = Json.writes[AnnotationList]

}

