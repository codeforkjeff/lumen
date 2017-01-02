package lumen

import javax.inject.{Inject, Singleton}

import com.github.tototoshi.play2.scalate.Scalate
import lumen.query.SearchBuilder
import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.common.SolrDocument
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request}

import scala.collection.JavaConverters._

/**
  * Composes and coordinates app-level objects, including
  * centralizing their creation, which is necessary for customization
  * (i.e. being able to plug in custom classes for things).
  */
@Singleton
class LumenApp @Inject() (val config: Config, val messagesApi: MessagesApi, val scalate: Scalate) extends I18nSupport {

  private def initScalate() = {
    // these settings are the only way I could get scalate to
    // NOT reload if nothing changed; activator/sbt reloads the app
    // on changes anyway, so changes always get picked up.
    // but this doesn't seem right.
    scalate.engine.allowCaching = true
    scalate.engine.allowReload = false

    val availableTemplateTypes = Seq("jade", "scaml", "mustache", "ssp")
    val defaultTemplates = availableTemplateTypes.map("lumen/layouts/default." + _)
    scalate.engine.layoutStrategy = new DefaultLayoutStrategy(scalate.engine, defaultTemplates: _*)
  }

  initScalate()

  var templateCache: StringMap = Map[String, String]()

  val repository = config.repositoryFactory(config)

  val layout = config.layoutFactory()

  def createQueryBuilder(): SearchBuilder = {
    config.searchBuilderFactory()
  }

  def createDocumentPresenter(reqCtx: RequestContext, doc: SolrDocument, view: String): DocumentPresenter = {
    val factory = config.documentPresenterFactory(view)
    factory(reqCtx, doc)
  }

  def createConstraints(reqCtx: RequestContext): Constraints =
    config.constraintsFactory(reqCtx: RequestContext)

  /**
    * @param searchState
    * @param facetField
    * @param displayFacet
    * @param limit this will be either facetField.limit or facetField.moreLimit,
    *              depending on where this paginator is being used
    * @return
    */
  def createFacetPaginator(searchState: SearchState, facetField: FacetFieldConfig, displayFacet: FacetField, limit: Int): FacetPaginator = {
    val pageParam = config.facetPaginationRequestKeys("page")
    val pageParamValue = if(searchState.hasParam(pageParam)) searchState.getParamValue(pageParam) else "1"
    val offset = (pageParamValue.toInt - 1) * limit
    new FacetPaginator(displayFacet.getValues().asScala.toList, sort=searchState.getMoreFacetsSort, offset=offset, limit=limit)
  }

}
