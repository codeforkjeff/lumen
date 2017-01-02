package lumen.response

import scala.collection.mutable
import scala.collection.JavaConversions
import lumen.Config
import org.apache.solr.client.solrj.response.{FacetField, PivotField, QueryResponse}

class Response(val config: Config, val solrResponse: QueryResponse) {

  val facets = parseFacets() ++ parseFacetQuery()

  def parseFacets(): Map[String, FacetField] = {
    if(solrResponse.getFacetFields() != null) {
      val buf = JavaConversions.asScalaBuffer(solrResponse.getFacetFields())
      buf.map(field => field.getName -> field).toMap
    } else {
      Map.empty
    }
  }

  def parseFacetQuery(): Map[String, FacetField] = {
    if(solrResponse.getFacetQuery() != null) {
      val results = mutable.Map[String, FacetField]()
      val buf = JavaConversions.mapAsScalaMap(solrResponse.getFacetQuery()).toMap
      buf.foreach(queryToCount => {
        val (query, count) = queryToCount
        val fieldNameToFieldConfig = config.facetFields.find(facetNameToField => {
          val fieldConfig = facetNameToField._2
          fieldConfig.getLabelForQuery(query).isDefined
        }).orNull

        val (fieldName, fieldConfig) = fieldNameToFieldConfig

        val responseField = if(results.exists(_._1 == fieldName)) {
          results(fieldName)
        } else {
          val newField = new FacetField(fieldName)
          results += (fieldName -> newField)
          newField
        }
        responseField.add(query, count.toLong)
      })
      results.toMap
    } else {
      Map.empty
    }
  }

  /** Looks only at the top-level for the passed-in pivot facet "key"
    * (which is a comma-sep list of fieldnames)
    */
  def getPivotFacet(key: String): List[PivotField] = {
    JavaConversions.asScalaBuffer(solrResponse.getFacetPivot.get(key)).toList
  }

  def isEmpty(): Boolean = solrResponse.getResults.size == 0

  // TODO: handle grouping

  def documentCounterWithOffset(idx: Long, offset: Long = -1): Long = {
    val _offset = if (offset == -1) solrResponse.getResults().getStart() else 0
   // unless render_grouped_response?
    idx + 1 + _offset
  }

}

object Response {
  def apply(config: Config, solrResponse: QueryResponse) = new Response(config, solrResponse)
}
