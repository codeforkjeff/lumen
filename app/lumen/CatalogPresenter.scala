package lumen

import org.apache.solr.common.SolrDocument
import org.fusesource.scalate.TemplateEngine
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}

import scala.collection.JavaConverters._

/**
  * Code having to do with general presentation
  */
// blacklight/catalog_helper_behavior.rb
class CatalogPresenter(val req: Request[AnyContent],
                       val app: LumenApp,
                       val messages: Messages,
                       val engine: TemplateEngine) {

  val config: Config = app.config

  def renderSearchToPageTitle: String = {
    val defaultSearchField = config.defaultSearchField
    val searchField = req.getQueryString("search_field").orNull
    val q: String = req.getQueryString("q").orNull
    val constraintsQ: List[String] = if(q != null) {
      val qLabel: String = if (!((defaultSearchField != null) && searchField == defaultSearchField.key)) {
        labelForSearchField(req.getQueryString("search_field").orNull)
      } else {
        null
      }
      if (qLabel != null) {
        List(messages("lumen.search.page_title.constraint", qLabel, q))
      } else {
        List(q)
      }
    } else {
      List.empty
    }

    val searchState = new SearchState(config, req.queryString)
    val facets = searchState.getFacetFields

    val constraintsFacets = if(facets.nonEmpty) {
      // constraints += params['f'].to_unsafe_h.collect { |key, value| render_search_to_page_title_filter(key, value) }
      facets.map((facet) => renderSearchToPageTitleFilter(facet, searchState.getFacetValues(facet))).toList
    } else {
      List.empty
    }

    val constraints: List[String] = constraintsQ ++ constraintsFacets

    constraints.mkString(" / ")
  }

  def renderSearchToPageTitleFilter(facet: FacetFieldConfig, values: Seq[String]): String = {
    val label = facet.label
    if(values.length >= 3) {
      messages("lumen.search.page_title.many_constraint_values", values.size)
    } else {
      messages("lumen.search.page_title.constraint", facet.label,
        values.mkString(", "))
    }
  }

  def labelForSearchField(key: String): String = {
    val field = config.getSearchField(key)
    if (field != null && field.label != null) {
      field.label
    } else {
      messages("lumen.search.fields.default")
    }
  }

  def documentMainContentRenderParams: RenderParams =
    new RenderParams("/lumen/catalog/show_main_content.ssp", Map.empty)

  def documentSidebarRenderParams: RenderParams =
    new RenderParams("/lumen/catalog/show_sidebar.ssp", Map.empty)

  def documentIndexViewType: String = {
    val viewParam = req.getQueryString("view").getOrElse(req.session.get("preferred_view").orNull)
    if (viewParam != null && config.viewTypes.keys.exists(_ == viewParam)) {
      viewParam
    } else {
      defaultDocumentIndexViewType
    }
  }

  def documentIndexViews: Map[String, ViewConfig] = config.viewTypes
  /*
  TODO:
    config.viewTypes.filter(_.) select do |k, config|
      should_render_field? config
    end
  */

  def defaultDocumentIndexViewType: String =
    documentIndexViews.find(pair => pair._2.isDefaultViewType).get._1

  def documentClass(document: SolrDocument): String = {
    val types = document.getFieldValues(config.viewConfigs.get(documentIndexViewType).map(_.displayTypeField).head)
    types.asScala.map(_.toString).map(documentClassPrefix + Util.parameterize(_)).mkString(" ")
  }

  def documentClassPrefix = "lumen-"

  // code ported from render_partials_helper.rb goes here

  def documentIndex: RenderParams = documentIndexWithView(documentIndexViewType)

  def documentIndexWithView(view: String, extraParams: StringMap = Map.empty): RenderParams = {
    val cacheKey = List("index", view).mkString("_")
    val cachedTemplateOpt = app.templateCache.get(cacheKey)
    val template = if(cachedTemplateOpt.isEmpty) {
      val t = findDocumentIndexTemplateWithView(view)
      app.templateCache = app.templateCache.updated(cacheKey, t.get)
      t.get
    } else {
      cachedTemplateOpt.get
    }

    new RenderParams(template, extraParams)
  }

  // this method replaces render_document_partials
  def documentTemplatesRenderParams(document: SolrDocument, templates: List[String], extraParams: StringMap = Map.empty): List[RenderParams] = {
    templates.map(template => {
      documentTemplateRenderParams(document, template, extraParams)
    })
  }

  // this method replaces render_document_partial
  def documentTemplateRenderParams(document: SolrDocument, baseName: String, extraParams: StringMap = Map.empty): RenderParams = {
    val format = documentPartialName(document, baseName)
    val viewType = documentIndexViewType

    val cacheKey = List("show", viewType, baseName, format).mkString("_")
    val cachedTemplateOpt = app.templateCache.get(cacheKey)
    val template = if(cachedTemplateOpt.isEmpty) {
      val t = findDocumentShowTemplateWithView(viewType, baseName, format)
      app.templateCache = app.templateCache.updated(cacheKey, t.get)
      t.get
    } else {
      cachedTemplateOpt.get
    }

    new RenderParams(template, extraParams)
  }

  def documentPartialName(document: SolrDocument, baseName: String): String = {
    val viewConfig = config.viewConfigs("show")

    val displayType1: String = if(document.get(viewConfig.displayTypeField) != null) {
      document.get(viewConfig.displayTypeField).toString
    } else {
      null
    }

    val displayType2 = if(displayType1 == null || displayType1.isEmpty) {
      "default"
    } else {
      displayType1
    }

    typeFieldToPartialName(document, displayType2)
  }

  def typeFieldToPartialName(document: SolrDocument, displayType: String): String = {
    Util.parameterize(displayType.replace('-', '_'))
  }

  /**
    * @return absolute path to a template that exists and can be loaded by template engine
    */
  def findDocumentShowTemplateWithView(viewType: String, baseName: String, format: String): Option[String] =
    documentPartialPathTemplates(baseName, format, viewType).find(template => {
      Logger.info(s"Looking for document partial ${template}")
      engine.canLoad(template)
    })

  def findDocumentIndexTemplateWithView(view: String): Option[String] = {
    documentIndexPathTemplates(view).find(template => {
      Logger.info(s"Looking for document index partial ${template}")
      engine.canLoad(template)
    })
  }

  // Note that we don't support some of the legacy template names that BL does
  def documentPartialPathTemplates(actionName: String, format: String, indexViewType: String): List[String] = List(
    s"/lumen/catalog/${actionName}_${indexViewType}_${format}",
    s"/lumen/catalog/${actionName}_${format}",
    s"/lumen/catalog/${actionName}_default",
    s"/lumen/catalog/${actionName}_partials/${format}",
    s"/lumen/catalog/${actionName}_partials/default"
  ).map(_ + ".ssp")

  def documentIndexPathTemplates(indexViewType: String) = List(
    s"/lumen/catalog/document_${indexViewType}",
    s"/lumen/catalog/document_list"
  ).map(_ + ".ssp")

}

object CatalogPresenter {
  def apply(reqCtx: RequestContext, engine: TemplateEngine) = new CatalogPresenter(
    reqCtx.req,
    reqCtx.app,
    reqCtx.messages,
    engine)
}
