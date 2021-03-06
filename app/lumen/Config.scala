package lumen

import lumen.query.SearchBuilder
import lumen.response.Response
import org.apache.solr.client.solrj.response.FacetField.Count
import org.apache.solr.common.SolrDocument
import org.fusesource.scalate.TemplateEngine

class FieldConfig(val fieldname: String,
                  val label: String) {

}

class ShowFieldConfig(override val fieldname: String,
                      override val label: String,
                      val includeInRequest: Boolean = true,
                      val solrParams: Map[String, String] = Map.empty) extends FieldConfig(fieldname, label) {

}

class IndexFieldConfig(override val fieldname: String,
                       override val label: String,
                       val includeInRequest: Boolean = true,
                       val solrParams: Map[String, String] = Map.empty,
                       val highlight: Boolean = false) extends FieldConfig(fieldname, label) {

}

/**
  * 'fieldname' is the value passed to the Solr sort param.
  */
class SortFieldConfig(override val fieldname: String,
                      override val label: String,
                      val default: Boolean = false) extends FieldConfig(fieldname, label) {

}

class SearchFieldConfig(override val fieldname: String,
                        override val label: String,
                        val default: Boolean = false,
                        val keyField: String = null,
                        val solrParameters: Map[String, String] = Map.empty,
                        val solrLocalParameters: Map[String, String] = Map.empty,
                        val qt: String = null) extends FieldConfig(fieldname, label) {

  def key: String = if(keyField != null) keyField else fieldname
}

// 'url_method'
class FacetFieldConfig(override val fieldname: String,
                       override val label: String,
                       // rename 'partial' to view: this should be a template filename
                       val view: Option[String] = None,
                       val show: Boolean = true,
                       val collapse: Boolean = true,
                       val limit: Int = -1, // -1 = unlimited
                       val moreLimit: Int = 20, // used by "more" link, new in Lumen, not in Blacklight
                       val keyField: String = null,
                       val query: List[FacetQueryItem] = List.empty,
                       val includeInRequest: Boolean = false,
                       val pivot: List[String] = List.empty,
                       val sort: String = null,
                       val solrParams : Map[String, String] = Map.empty,
                       val single: Boolean = false,
                       val indexRange: Option[Seq[String]] = None) extends FieldConfig(fieldname, label) {

  val tag: Option[String] = if(single) { Some(s"${key}_single") } else None

  val ex: Option[String] = if(single) { Some(s"${key}_single") } else None

  /**
    * getter that resolves the right value for 'key'
    * which is used for URL param name for this facet field
    *  @return
    */
  def key: String = if(keyField != null) keyField else fieldname

  /** returns the label field form the FacetQueryItem
    * whose fq() result matches passed-in queryStr
    **/
  def getLabelForQuery(queryStr: String): Option[String] = {
    query.find(_.fq() == queryStr).map(_.label)
  }

  /**
    * @param value the facet value as passed around in URL params
    *              (i.e. as generated by getFacetValue())
    * @return
    */
  def getLabelForFacetValue(value: String): String = {
    if(query.nonEmpty) {
      query.find(_.paramValue == value).map(_.label).get
    } else {
      value
    }
  }

  def getDisplayValue(value: String): String = {
    if(query.nonEmpty) {
      getLabelForQuery(value).get
    } else {
      value
    }
  }

  /**
    * Translate the value found in Count object into a facet value appropriate
    * for passing around in URL parameters. This handles the case of query facets.
    */
  def getFacetValue(count: Count): String = {
    if(query.nonEmpty) {
      query.find(_.fq() == count.getName).map(_.paramValue).get
    } else {
      count.getName
    }
  }

  def getFacetQueryItem(value: String): Option[FacetQueryItem] = {
    query.find(item => item.paramValue == value)
  }

  def isPivot: Boolean = pivot.nonEmpty

  def pivotKey: String = pivot.mkString(",")
}

/**
  * @param paramValue URL param value to use for passing facet query around
  * @param label
  * @param fq
  */
case class FacetQueryItem(paramValue: String, label: String, fq: () => String)

case class ViewConfig(name: String,
                      docPresenterFactory: DocumentPresenterFactory,
                      partials: List[String] = List.empty,
                      titleField: String = null,
                      displayTypeField: String = null,
                      isViewType: Boolean = false,
                      isDefaultViewType: Boolean = false,
                      documentActions: Seq[DocumentActionFactory] = List.empty) {

}

trait Config {

  // Search request configuration
  val httpMethod = "get"
  val solrPath = "select"
  val defaultSolrParams: StringMap = Map.empty
  val qt: Option[String] = None

  val connection_url = "http://localhost:8983/solr/lumen-core"

  // Single document request configuration
  val documentSolrRequestHandler = "document"
  val documentSolrPath: Option[String] = None
  val documentUniqueIdParam = "id"
  val defaultDocumentSolrParams: StringMap = Map.empty

  val addFacetFieldsToSolrRequest = true
  val addFieldConfigurationToSolrRequest = true
  val defaultPerPage = 10
  val maxPerPage = 100

  val perPage = List(10,20,50,100)

  /**
    * For Facet Pagination: this used to live in FacetPaginator.
    * What request keys will we use for the parameters need. Need to
    * make sure they do NOT conflict with catalog/index request params,
    * and need to make them accessible in a list so we can easily
    * strip em out before redirecting to catalog/index.
    */
  def facetPaginationRequestKeys =
    Map(
      "sort" -> "facet.sort",
      "page" -> "facet.page",
      "prefix" -> "facet.prefix"
    )

  val showFieldsOrdered: Seq[ShowFieldConfig] = defineShowFields
  val showFields: Map[String, ShowFieldConfig] = showFieldsOrdered.map(f => f.fieldname -> f).toMap

  val indexFieldsOrdered: Seq[IndexFieldConfig] = defineIndexFields
  val indexFields: Map[String, IndexFieldConfig] = indexFieldsOrdered.map(f => f.fieldname -> f).toMap

  val searchFieldsOrdered: Seq[SearchFieldConfig] = defineSearchFields
  val searchFields: Map[String, SearchFieldConfig] = searchFieldsOrdered.map(f => f.fieldname -> f).toMap

  val facetFieldsOrdered: Seq[FacetFieldConfig] = defineFacetFields
  val facetFields: Map[String, FacetFieldConfig] = facetFieldsOrdered.map(f => f.fieldname -> f).toMap

  val sortFieldsOrdered: Seq[SortFieldConfig] = defineSortFields
  val sortFields: Map[String, SortFieldConfig] = sortFieldsOrdered.map(f => f.fieldname -> f).toMap

  def searchBuilderFactory: SearchBuilderFactory = SearchBuilder.apply

  def constraintsFactory: ConstraintsFactory = Constraints.apply

  def repositoryFactory: RepositoryFactory =
    (config: Config) => {
      Repository.apply(config: Config)
    }

  def responseFactory: ResponseFactory = Response.apply

  def documentPresenterFactory(view: String): DocumentPresenterFactory = {
    (reqCtx: RequestContext, doc: SolrDocument) => {
      reqCtx.app.config.viewConfigs.get(view)
        .map(_.docPresenterFactory(reqCtx, doc))
        .head
    }
  }

  def layoutFactory: LayoutFactory = Layout.apply

  def catalogPresenterFactory(reqCtx: RequestContext, engine: TemplateEngine): CatalogPresenterFactory =
    () => CatalogPresenter.apply(reqCtx, engine)

  /**
    * BL does some nutty hash merging between 'viewConfigs'
    * and 'view' so that list, atom, rss are treated as subtypes of 'index',
    * and inherit from it. 'show' also inherits from 'index'.
    * Here, we model everything as viewConfigs.
    */
  private val indexViewConfig = ViewConfig(
    name = "index",
    docPresenterFactory = IndexDocumentPresenter.apply,
    partials = List("index_header", "thumbnail", "index"),
    displayTypeField = "format")

  def defineViewConfigs = List(
    indexViewConfig,
    indexViewConfig.copy(
      name = "show",
      docPresenterFactory =ShowDocumentPresenter.apply,
      partials = List("show_header", "show"),
      documentActions = List(BookmarkAction.apply, EmailAction.apply)),
    indexViewConfig.copy(
      name = "list",
      isViewType = true,
      isDefaultViewType = true),
    indexViewConfig.copy(
      name = "atom",
      isViewType = true,
      partials = List("document")),
    indexViewConfig.copy(
      name = "rss",
      isViewType = true,
      partials = List("document"))
  )

  val viewConfigsOrdered: List[ViewConfig] = defineViewConfigs
  def viewConfigs: Map[String, ViewConfig] = viewConfigsOrdered.map(vc => vc.name -> vc).toMap

  // this corresponds to 'view' key in BL config
  def viewTypes: Map[String, ViewConfig] = viewConfigs.filter(pair => pair._2.isViewType)

  def defaultSortField: SortFieldConfig = {
    sortFields.values.find(_.default).getOrElse(
      sortFieldsOrdered.headOption.orNull)
  }

  def defaultSearchField: SearchFieldConfig = {
    searchFields.values.find(_.default).getOrElse(
      searchFieldsOrdered.headOption.orNull)
  }

  def defineShowFields: Seq[ShowFieldConfig] = List.empty

  def defineIndexFields: Seq[IndexFieldConfig] = List.empty

  def defineSearchFields: Seq[SearchFieldConfig] = List.empty

  def defineFacetFields: Seq[FacetFieldConfig] = List.empty

  def defineSortFields: Seq[SortFieldConfig] = List.empty

  def getFacetField(field: String): lumen.FacetFieldConfig =
    facetFields.getOrElse(field, null)

  def getSearchField(field: String): lumen.SearchFieldConfig =
    searchFields.getOrElse(field, null)

}
