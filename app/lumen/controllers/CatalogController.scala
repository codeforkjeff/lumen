package lumen.controllers

import com.github.tototoshi.play2.scalate.Scalate
import com.google.inject.Inject
import lumen._
import play.api.Logger
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, ActionBuilder, Request, Result}

import scala.concurrent.Future

case class SearchFormData(q: Option[String], searchField: Option[String])

case class Timer[A](action: Action[A]) extends Action[A] {
  lazy val parser = action.parser
  def apply(request: Request[A]): Future[Result] = {
    val start = System.currentTimeMillis()
    val retVal = action(request)
    Logger.info(s"Request ${request.uri} took ${System.currentTimeMillis() - start}ms")
    retVal
  }
}

class CatalogController @Inject() (override val messagesApi: MessagesApi,
                                   override val scalate: Scalate,
                                   override val config: Config,
                                   override val app: LumenApp)
  extends BaseController(messagesApi, scalate, config, app) {

  val searchForm = Form(
    Forms.mapping(
      "search_field" -> Forms.optional(Forms.text),
      "q" -> Forms.optional(Forms.text))(SearchFormData.apply)(SearchFormData.unapply)
  )

  def index() = Timer {
    Action { implicit req =>
      val searchFormData = searchForm.bindFromRequest.get

      val searchState = new SearchState(config, req.queryString)

      val solrResponse = new Search(app).searchResults(req)

      Ok(scalate.render("lumen/catalog/index.ssp",
        defaultLayoutMap() ++
          Map("title" -> "Lumen",
            "searchForm" -> searchForm,
            "searchState" -> searchState,
            "response" -> solrResponse
          )
      ))
    }
  }

  def show(id: String) = Action { implicit req =>
    val searchFormData = searchForm.bindFromRequest.get

    val solrResponse = new Search(app).fetch(id)

    // TODO
    //val searchSession = new SearchSession()
    //searchSession.setup_next_and_previous_documents()

    // TODO: handle additional formats

    val doc = solrResponse.solrResponse.getResults.get(0)
    val docPresenter = app.createDocumentPresenter(createRequestContext, doc, "show")

    Ok(scalate.render("lumen/catalog/show.ssp",
      defaultLayoutMap() ++
      Map("title" -> "Lumen",
        "searchForm" -> searchForm,
//        "searchSession" -> searchSession,
        "response" -> solrResponse,
        "document" -> doc,
        "docPresenter" -> docPresenter
      )
    ))
  }

  def facet(field: String) = Action { implicit req =>
    val facet = config.getFacetField(field)
    val response = new Search(app).getFacetFieldResponse(req, facet)
    val searchState = new SearchState(config, req.queryString)
    val facetFields = FacetFields(createRequestContext)
    val displayFacet = response.facets(field)
    val paginator = app.createFacetPaginator(searchState, facet, displayFacet, facet.moreLimit)
    Ok(scalate.render("lumen/catalog/facet.ssp",
      defaultLayoutMap() ++
        Map(
          "layout" -> "",
          "response" -> response,
          "searchState" -> searchState,
          "facetField" -> facet,
          "facetFields" -> facetFields,
          "displayFacet" -> displayFacet,
          "paginator" -> paginator
        )
    ))
  }

}
