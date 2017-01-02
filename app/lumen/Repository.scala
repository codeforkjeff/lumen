package lumen

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import play.api.Logger

class Repository(val config: Config) {

  val clientBuilder = new HttpSolrClient.Builder(config.connection_url)

  val client: HttpSolrClient = clientBuilder.build()

  def search(search: SolrQuery): QueryResponse = {
    if(config.qt.nonEmpty) {
      search.set("qt", config.qt.get)
    }
    runQuery(search)
  }

  def find(id: String, params: StringMap = Map.empty): QueryResponse = {
    val search = new SolrQuery()
    val docParams =
      Map("qt" -> config.documentSolrRequestHandler) ++
      config.defaultDocumentSolrParams ++
      params ++
      Map(config.documentUniqueIdParam -> id)

    docParams.foreach(pair => search.set(pair._1, pair._2))

    runQuery(search)
  }

  private def runQuery(query: SolrQuery): QueryResponse = {
    Logger.info("Solr query = " + query)
    client.query(query)
  }

}

object Repository {
  def apply(config: Config) = new Repository(config)
}
