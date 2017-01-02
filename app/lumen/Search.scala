package lumen

import lumen.query.SearchBuilderContext
import lumen.response.Response
import org.apache.solr.client.solrj.response.QueryResponse
import play.api.Logger
import play.api.mvc.{AnyContent, Request}

/**
  * Entry point for doing all kinds of searches
  * @param app
  */
class Search(val app: LumenApp) {

  private def createSearchBuilderContext(req: Request[AnyContent], browseFacet: String = null): SearchBuilderContext = {
    new SearchBuilderContext(app.config, req, browseFacet = browseFacet)
  }

  /**
    * App-level search
    */
  def searchResults(req: Request[AnyContent]): Response = {
    val qb = app.createQueryBuilder()
    val q = qb.build(createSearchBuilderContext(req))
    val (solrResponse, elapsed) = Util.timeFunction[QueryResponse](() => app.repository.search(q))
    Logger.info(s"Solr request time = ${elapsed}ms")
    // TODO:
    /*
    case
    when (response.grouped? && grouped_key_for_results)
      [response.group(grouped_key_for_results), []]
    when (response.grouped? && response.grouped.length == 1)
      [response.grouped.first, []]
    else
      [response, response.documents]
    end
    */
    app.config.responseFactory(app.config, solrResponse)
  }

  def fetch(id: String, extraControllerParams: Map[String, String] = Map.empty): Response = {
    val solrResponse = app.repository.find(id, extraControllerParams)
    app.config.responseFactory(app.config, solrResponse)
  }

  // TODO: def fetch_many(idList: List[String], search_state.to_h, extra_controller_params)

  def getFacetFieldResponse(req: Request[AnyContent], facetField: FacetFieldConfig, extraParams: StringMap = Map.empty): Response = {
    val qb = app.createQueryBuilder()
    val searchCtx = createSearchBuilderContext(req, browseFacet = facetField.fieldname)
    qb.prepend((ctx, query) => {
      extraParams.foreach(pair => query.set(pair._1, pair._2))
      query
    })
    val q = qb.build(searchCtx)
    val solrResponse = app.repository.search(q)
    app.config.responseFactory(app.config, solrResponse)
  }

}
