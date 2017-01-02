package lumen

import play.api.i18n.Messages

/**
  * Logic for determining and displaying the current search constraints.
  * This is primarily used for rendering the box at the top of
  * the search results page.
  */
class Constraints(val app: LumenApp,
                  val messages: Messages,
                  val searchState: SearchState) {

  def queryHasConstraints =
    searchState.hasParamValue("q") || searchState.hasFacets

  /**
    * Return a label for the currently selected search field.
    * If no "search_field" or the default (e.g. "all_fields") is selected, then return nil
    * Otherwise grab the label of the selected search field.
    */
  def constraintQueryLabel: Option[String] = {
    if (!searchState.isDefaultSearchField) {
      Some(labelForSearchField(searchState.getParamValue("search_field")))
    } else {
      None
    }
  }

  /** is this method constraint-specific? I can't decide. */
  def labelForSearchField(key: String): String = {
    val field = app.config.searchFieldsOrdered.find(_.key == key).orNull
    if(field != null && field.label != null) {
      field.label
    } else {
      messages("lumen.search.fields.default")
    }
  }

  def labelForFacetField(key: String): Option[String] = {
    val field = app.config.facetFieldsOrdered.find(_.key == key).orNull
    if(field != null) {
      val i18nKeys = List(
        s"lumen.search.fields.facet.${field.fieldname}",
        s"lumen.search.fields.${field}")
      val key = i18nKeys.find(k => messages.isDefinedAt(k)).orNull
      if (key != null) {
        Some(messages(key))
      } else {
        Option(field.label).orElse(Some(field.fieldname))
      }
    } else {
      None
    }
  }

  def getSearchQuery: String = searchState.getParamValue("q")

  def hasSearchQuery: Boolean = {
    val q = getSearchQuery
    if(q == null) false else q.nonEmpty
  }

  def removeConstraintUrl: String = {
    // TODO: catalog url
    "/catalog?" + Util.queryString(searchState.clearSearchQuery.params)
  }

  def facetDisplayValue(field: FacetFieldConfig, value: String) = {
    field.getLabelForFacetValue(value)
  }

  def queryRenderParams = new RenderParams(
    "/lumen/catalog/constraints_element.ssp",
    Map("label" -> constraintQueryLabel,
      "value" -> getSearchQuery,
      "removeUrl" -> removeConstraintUrl,
      "classes" -> List("query")))

  def filterRenderParams(facetField: FacetFieldConfig, facetValue: String) = new RenderParams(
    "/lumen/catalog/constraints_element.ssp",
    Map("label" -> labelForFacetField(facetField.fieldname),
      "value" -> facetDisplayValue(facetField, facetValue),
      "removeUrl" -> searchState.pathWithoutFacet(facetField, facetValue),
      "classes" -> List("filter", "filter-" + Util.parameterize(facetField.fieldname))))

}

object Constraints {
  def apply(reqCtx: RequestContext) = new Constraints(
    reqCtx.app,
    reqCtx.messages,
    new SearchState(reqCtx.app.config, reqCtx.req.queryString)
  )
}
