package lumen

import org.apache.solr.client.solrj.SolrQuery

package object query {
  type RequestToQueryF = (SearchBuilderContext, SolrQuery) => SolrQuery

}
