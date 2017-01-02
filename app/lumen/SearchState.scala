package lumen

import org.apache.solr.client.solrj.response.FacetField.Count

/**
  * This is a wrapper around URL params so that we can make sense of them
  * as domain information, like what facets have been selected?
  * Also makes it easy to transform the existing set of params for generating
  * new URLs.
  */
case class SearchState(config: Config, params: Map[String, Seq[String]]) {

  def facetFieldNameAsParam(fieldname: String): String = "f." + fieldname

  def addFacetParamsAndRedirect(facetField: FacetFieldConfig, count: Count): SearchState = {
    addFacetParamsAndRedirect(facetField, facetField.getFacetValue(count))
  }

  def addFacetParamsAndRedirect(facetField: FacetFieldConfig, value: String): SearchState = {
    val fieldname = facetFieldNameAsParam(facetField.fieldname)
    val values = params.find(_._1 == fieldname).getOrElse(("", List.empty[String]))._2
    val newValues = if(facetField.single && params.exists(_._1 == fieldname)) {
      Seq(value)
    } else {
      values :+ value
    }
    copy(params = params -- config.facetPaginationRequestKeys.values.toList
      + (fieldname -> newValues)
      + ("page" -> List.empty[String]))
  }

  def removeFacetParam(field: FacetFieldConfig, count: Count): SearchState =
    removeFacetParam(field, count.getName)

  def removeFacetParam(field: FacetFieldConfig, value: String): SearchState = {
    val fieldname = facetFieldNameAsParam(field.fieldname)
    val values = params.find(_._1 == fieldname).getOrElse(("", List.empty[String]))._2
    val newValues = values.diff(List(value))
    copy(params = params + (fieldname -> newValues) + ("page" -> List.empty[String]))
  }

  def getSearchQuery: Option[String] = {
    params.getOrElse("q", List.empty[String]).headOption
  }

  def clearSearchQuery: SearchState = {
    copy(params = params - "q")
  }

  def addParam(field: String, value: String): SearchState = {
    val values = params.find(_._1 == field).getOrElse(("", List.empty[String]))._2
    val newValues = values :+ value
    copy(params = params + (field -> newValues))
  }

  /** set parameter, overriding if it already exists */
  def setParam(field: String, value: String): SearchState = {
    copy(params = params + (field -> List(value)))
  }

  def getParamValue(field: String): String = {
    getParamValues(field).headOption.orNull
  }

  def getParamValues(field: String): Seq[String] = {
    params.filterKeys(_ == field).headOption.getOrElse(("", List.empty[String]))._2
  }

  /** returns true if the parameter is present at all in the query string, irrespective of its value */
  def hasParam(field: String): Boolean = params.contains(field)

  /** returns true if the parameter's value has length > 0 */
  def hasParamValue(field: String): Boolean =
    params.getOrElse(field, List.empty).headOption.exists(_.length > 0)

  /** returns true if this search filtered on any facets */
  def hasFacets: Boolean =
    params.filterKeys(key => key.startsWith("f.")).nonEmpty

  /** returns the FacetFieldConfig objects for which this SearchState has filters */
  def getFacetFields: Seq[FacetFieldConfig] = {
    val facets = params.filterKeys(key => key.startsWith("f.")).keys.toList.map(_.substring(2))
    config.facetFieldsOrdered.filter(f => facets.contains(f.fieldname))
  }

  def getFacetValues(field: FacetFieldConfig): Seq[String] =
    getParamValues("f." + field.fieldname)

  def selectedSearchField: String = {
    val values = params.filterKeys(_ == "search_field").headOption.getOrElse(("", List.empty))._2
    values.headOption.orNull
  }

  // Is the search form using the default search field ("all_fields" by default)?
  def isDefaultSearchField: Boolean = {
    val selected = selectedSearchField
    selected == null || selected == defaultSearchField.key
  }

  def defaultSearchField: SearchFieldConfig = {
    val default = config.searchFields.values.find(_.default).orNull
    if(default != null) default else config.searchFieldsOrdered.headOption.orNull
  }

  def pathForFacet(facetField: FacetFieldConfig, count: Count): String = {
    val params = addFacetParamsAndRedirect(facetField, count).params
    // TODO: url for catalog
    "/catalog?" + Util.queryString(params)
  }

  def pathForFacet(facetField: FacetFieldConfig, value: String): String = {
    val params = addFacetParamsAndRedirect(facetField, value).params
    // TODO: url for catalog
    "/catalog?" + Util.queryString(params)
  }

  def pathWithoutFacet(facetField: FacetFieldConfig, count: Count): String =
    pathWithoutFacet(facetField, facetField.getFacetValue(count))

  def pathWithoutFacet(facetField: FacetFieldConfig, value: String): String = {
    val params = removeFacetParam(facetField, value).params
    // TODO: url for catalog
    "/catalog?" + Util.queryString(params)
  }

  /** link for new sort order in "more facets" */
  def pathForMoreFacetsSort(sort: String): SearchState = {
    copy(params = params -- config.facetPaginationRequestKeys.values.toList
      + (config.facetPaginationRequestKeys("sort") -> List(sort)))
  }

  def pathForMoreFacetsPrefix(prefix: String): SearchState = {
    copy(params = params
      + (config.facetPaginationRequestKeys("prefix") -> List(prefix)))
  }

  def pathForMoreFacetsClearPrefix: SearchState = {
    copy(params = params.filter(_._1 != config.facetPaginationRequestKeys("prefix")))
  }

  /** link for previous page in "more facets" */
  def pathForMoreFacetsPrevious(paginator: FacetPaginator): SearchState = {
    setParam(config.facetPaginationRequestKeys("page"), paginator.prevPage.toString)
      .setParam("_", System.currentTimeMillis().toString)
  }

  /** link for next page in "more facets" */
  def pathForMoreFacetsNext(paginator: FacetPaginator): SearchState = {
    setParam(config.facetPaginationRequestKeys("page"), paginator.nextPage.toString)
      .setParam("_", System.currentTimeMillis().toString)
  }

  /** returns the current sort order in "more facets" */
  def getMoreFacetsSort: String = {
    val sort = getParamValue(config.facetPaginationRequestKeys("sort"))
    if(sort != null) sort else "count"
  }

  def isMoreFacetsIndexSort: Boolean = getMoreFacetsSort == "index"

  def getMoreFacetsPrefix: Option[String] =
    Option(getParamValue(config.facetPaginationRequestKeys("prefix")))

  def getSortField: SortFieldConfig = {
    val s = getParamValue("sort")
    config.sortFieldsOrdered.find(_.fieldname == s).getOrElse(config.sortFieldsOrdered.headOption.orNull)
  }

  def getPerPage: String = {
    val perPage = getParamValue("per_page")
    if(perPage == null) {
      config.defaultPerPage.toString
    } else {
      perPage
    }
  }

  // returns a new SearchState containing params appropriates for a search form
  def forSearch(): SearchState = {
    val newParams = params -- SearchState.paramsToStripForNewSearch
    copy(params = newParams)
  }

  def hasSearchParameters: Boolean =
    getParamValue("q") != null ||
      getFacetFields.nonEmpty ||
      getParamValue("search_field") != null

  /**
    * If either sort or per_page changes, we need to reset the page
    */
  private def changeSortOrPerPage(param: String, value: String): SearchState = {
    val currentSort = getParamValue("sort")
    val perPage = getParamValue("per_page")
    val page = getParamValue("page")

    val newParams = params + (param -> List(value))

    if(page != null &&
      (currentSort != newParams.getOrElse("sort", List.empty[String]).headOption.orNull ||
        perPage != newParams.getOrElse("per_page", List.empty[String]).headOption.orNull)) {
      copy(params = newParams + ("page" -> List("1")))
    } else {
      copy(params = newParams)
    }
  }

  def changeSort(sortValue: String): SearchState =
    changeSortOrPerPage("sort", sortValue)

  def changePerPage(page: Int): SearchState =
    changeSortOrPerPage("per_page", page.toString)

  def path: String =
    "/catalog?" + Util.queryString(params)

  /**
    * Returns the search represented by this object, as a query string
    * that can be appended to some base URL.
    * @return
    */
  def queryString: String = Util.queryString(params)

}

object SearchState {

  val paramsToStripForNewSearch = List(
    "q", "search_field", "qt", "page", "utf8")

}
