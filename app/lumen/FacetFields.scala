package lumen

import lumen.response.Response
import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.client.solrj.response.FacetField.Count
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}

/**
  * Logic having to do with facet fields as defined in config and what's
  * in the http request. This should ONLY be used by view logic
  * (e.g. sidebar facet display and other views).
  *
  * TODO: should Response be part of constructor?
  */
class FacetFields(val req: Request[AnyContent],
                  val config: lumen.Config,
                  val messages: Messages,
                  val _searchState: Option[SearchState] = None) {

  val searchState = _searchState.getOrElse(new SearchState(config, req.queryString))
  val facets = parseFacetFields()

  def facetFieldNames: Seq[String] =
    config.facetFieldsOrdered.map(_.fieldname)

  /**
    * parse facets and their values from request
    * Rails convention is to use f[facet_name][]=value
    * we use f.facet_name=value
    */
  private def parseFacetFields(): Map[String, Seq[String]] = {
    req.queryString.keys.filter(_.startsWith("f.")).map((param) => {
      param.substring(2) -> req.queryString.getOrElse(param, null)
    }).toMap
  }

  def facetFieldInParams(field: String): Boolean = facets.contains(field)

  /** checks if a facet field and the value in the Count record exists in the params */
  def facetInParams(facetField: lumen.FacetFieldConfig, countRecord: Count): Boolean = {
    val value = facetField.getFacetValue(countRecord)
    facetInParams(facetField, value)
  }

  /** checks if a facet field and a particular value exists in the params */
  def facetInParams(facetField: lumen.FacetFieldConfig, value: String): Boolean = {
    val default = ("", List.empty)
    val values = facets.find(_._1 == facetField.fieldname).getOrElse(default)._2
    values.contains(value)
  }

  def shouldCollapseFacet(facet_field: lumen.FacetFieldConfig): Boolean =
    !facetFieldInParams(facet_field.fieldname) && facet_field.collapse

  def hasFacetValues(response: Response): Boolean = {
    facetsFromRequest(response).exists(display_facet => {
      val fieldName = facetConfigurationForField(display_facet.getName)
      !(display_facet.getValueCount == 0) && shouldRenderFacet(response, fieldName)
    })
  }

  def shouldRenderFacet(response: Response, facetConfig: FacetFieldConfig): Boolean = {
    if (facetConfig.isPivot) {
      val pivotFacetField = response.getPivotFacet(facetConfig.pivotKey)
      facetConfig.show && pivotFacetField.length > 0
    } else {
      val facetField = response.facets.get(facetConfig.fieldname).orNull
      facetConfig.show && facetField.getValueCount > 0
    }
  }

  def facetsFromRequest(response: Response): Seq[FacetField] = {
    config.facetFields.keys.map(response.facets.get(_).orNull).filter(_ != null).toList
  }

  def facetConfigurationForField(field: String): lumen.FacetFieldConfig =
    config.getFacetField(field)

  def facetFieldLabel(field: String) = {
    val fieldConfig = config.facetFields.getOrElse(field, null)
    messages(Seq(
      s"lumen.search.fields.facet.${fieldConfig.fieldname}",
      s"lumen.search.fields.${fieldConfig.fieldname}",
      if(fieldConfig.label.nonEmpty) fieldConfig.label else Util.humanize(field)
    ))
  }

  def facetFieldId(facet_field: lumen.FacetFieldConfig) = s"facet-${facet_field.key}"

  def facetDisplayValue(facetField: lumen.FacetFieldConfig, countRecord: Count): String =
    facetDisplayValue(facetField, countRecord.getName())

  def facetDisplayValue(facetField: lumen.FacetFieldConfig, value: String): String =
    facetField.getDisplayValue(value)

  def selectFacetLink(facetField: FacetFieldConfig, countRecord: Count): String = {
    selectFacetLink(searchState.pathForFacet(facetField, countRecord),
      facetDisplayValue(facetField, countRecord))
  }

  def selectFacetLink(facetField: FacetFieldConfig, value: String): String = {
    selectFacetLink(searchState.pathForFacet(facetField, value),
      facetDisplayValue(facetField, value))
  }

  def selectFacetLink(url: String, linkText: String): String = {
    s"""<a class="facet_select" href="${url}">${linkText}</a>"""
  }

  def removeFacetLink(facetField: FacetFieldConfig, countRecord: Count): String = {
    removeFacetLink(searchState.pathWithoutFacet(facetField, countRecord), facetDisplayValue(facetField, countRecord))
  }

  def removeFacetLink(facetField: FacetFieldConfig, value: String): String = {
    removeFacetLink(searchState.pathWithoutFacet(facetField, value), value)
  }

  def removeFacetLink(url: String, linkText: String): String = {
    s"""<span class="selected">${linkText}</span>
      <a class="remove" href="${url}"><span class="glyphicon glyphicon-remove"></span><span class="sr-only">[remove]</span></a>"""
  }

}

object FacetFields {
  def apply(reqCtx: RequestContext, searchState: Option[SearchState] = None) = {
    new FacetFields(reqCtx.req, reqCtx.app.config, reqCtx.messages, searchState)
  }
}
