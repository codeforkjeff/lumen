import lumen.query.SearchBuilder
import lumen.response.Response
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument

package object lumen {
  type RepositoryFactory = (Config) => Repository
  type ResponseFactory = (Config, QueryResponse) => Response
  type DocumentPresenterFactory = (RequestContext, SolrDocument) => DocumentPresenter
  type LayoutFactory = () => Layout
  type CatalogPresenterFactory = () => CatalogPresenter
  type ConstraintsFactory = (RequestContext) => Constraints
  type SearchBuilderFactory = () => SearchBuilder
  type DocumentActionFactory = (DocumentPresenter) => DocumentAction

  // use judiciously! not every data structure is a StringMap
  type StringMap = Map[String, String]
}
