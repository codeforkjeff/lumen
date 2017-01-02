package lumen

import org.apache.solr.common.SolrDocument
import play.api.i18n.Messages

import scala.collection.JavaConverters._

trait DocumentPresenter {
  val config: Config
  val messages: Messages
  val doc: SolrDocument
  val view: String

  // TODO: should be genericized and moved to subclass
  def shouldRenderIndexField(field: IndexFieldConfig): Boolean = {
    // TODO: should_render_field?(field_config, document) && document_has_value?(document, field_config)
    documentHasValue(field)
  }

  // TODO: should be genericized and moved to subclass
  def shouldRenderShowField(field: ShowFieldConfig): Boolean = {
    // should_render_field?(field_config, document) && document_has_value?(document, field_config)
    documentHasValue(field)
  }

  def documentHasValue(field: FieldConfig): Boolean = {
    doc.getFieldValue(field.fieldname) != null
  }

  def itemType: String = "http://schema.org/Thing"

  def indexFields = config.indexFieldsOrdered

  // Used in the document list partial (search view) for creating a link to the document show action
  def documentShowLinkField: String = {
    val vc = config.viewConfigs.get(view).orNull
    val field = if(vc != null && vc.titleField != null) {
      config.indexFields.get(vc.titleField).orNull
    } else {
      config.indexFieldsOrdered.head
    }
    if(field != null) {
      val value = doc.getFieldValue(field.fieldname)
      if(value != null) value.toString else null
    } else {
      null
    }
  }

  def fieldValue(field: String): String = {
    fieldValues(field).mkString(", ")
  }

  def fieldValues(field: String): List[String] = {
    // TODO: delegate to FieldPresenter
    doc.getFieldValues(field).asScala.map(_.toString).toList
  }

  // TODO: should be genericized and moved to subclass
  def showFields =
    config.showFieldsOrdered

  // TODO: should be genericized and moved to subclass
  def showFieldLabel(field: FieldConfig): String = {
    val labelText = messages(Seq(
      s"lumen.search.fields.show.${field.fieldname}",
      s"lumen.search.fields.${field.fieldname}",
      if(field.label.nonEmpty) field.label else Util.humanize(field.fieldname)))
    messages("lumen.search.show.label", labelText)
  }

  def indexFieldLabel(field: FieldConfig): String = {
    // TODO: need to get view type properly
    val documentIndexViewType = "list"

    val labelText = messages(Seq(
      s"lumen.search.fields.index.${field.fieldname}",
      s"lumen.search.fields.${field.fieldname}",
      if(field.label.nonEmpty) field.label else Util.humanize(field.fieldname)))

    messages(Seq(
      s"lumen.search.index.${documentIndexViewType}.label",
      "lumen.search.index.label"),
      labelText)
  }

  def heading: String = {
    val field = config.viewConfigs.get(view)
      .map(_.titleField)
      .find(doc.getFieldValue(_) != null)
      .getOrElse(config.documentUniqueIdParam)
    fieldValues(field).mkString(", ")
  }

  def getId: String = {
    doc.getFieldValue(config.documentUniqueIdParam).toString()
  }
}

class IndexDocumentPresenter(override val config: Config, override val messages: Messages, override val doc: SolrDocument) extends DocumentPresenter {
  override val view = "index"
}

object IndexDocumentPresenter {
  def apply(reqCtx: RequestContext, doc: SolrDocument) = new IndexDocumentPresenter(reqCtx.app.config, reqCtx.messages, doc)
}

class ShowDocumentPresenter(override val config: Config, override val messages: Messages, override val doc: SolrDocument) extends DocumentPresenter {
  override val view = "show"
}

object ShowDocumentPresenter {
  def apply(reqCtx: RequestContext, doc: SolrDocument) = new ShowDocumentPresenter(reqCtx.app.config, reqCtx.messages, doc)
}
