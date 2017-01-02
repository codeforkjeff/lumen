package lumen.query

import lumen._
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.params.CommonParams
import play.api.mvc.{AnyContent, Request}

/**
  * A SearchBuilder consists of a pipeline of functions
  * to transform a Request into a SolrQuery.
  *
  * Blacklight::SearchHelper#search_results is where search happens
  */
class SearchBuilder(val pipeline: Seq[RequestToQueryF]) {

  def build(config: Config, req: Request[AnyContent]): SolrQuery = {
    val ctx = new SearchBuilderContext(config, req)
    build(ctx)
  }

  def build(ctx: SearchBuilderContext): SolrQuery = {
    pipeline.foldLeft(new SolrQuery())((acc, f) => {
      f(ctx, acc)
    })
  }

  def append(f: RequestToQueryF): SearchBuilder = {
    new SearchBuilder(pipeline :+ f)
  }

  def prepend(f: RequestToQueryF): SearchBuilder = {
    new SearchBuilder(f +: pipeline)
  }

}

class SearchBuilderContext(val config: Config, val req: Request[AnyContent], val browseFacet: String = null) {

  val searchState: SearchState = new SearchState(config, req.queryString)

  /**
    * convenience method for grabbing a value for a single url param
    * @param key
    * @return
    */
  def getParamString(key: String): String =
    req.getQueryString(key).getOrElse(null)
}

object SearchBuilder {

  val defaultPipeline = List[RequestToQueryF](
    defaultSolrParameters,
    addQueryToSolr,
    addFacetFqToSolr,
    addFacettingToSolr,
    addSolrFieldsToQuery,
    addPagingToSolr,
    addSortingToSolr,
    // TODO:
    // :add_group_config_to_solr
    addFacetPagingToSolr
  )

  def pipeline = defaultPipeline

  def apply(): SearchBuilder = {
    new SearchBuilder(pipeline)
  }

  def defaultSolrParameters(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    ctx.config.defaultSolrParams.foreach { case (key, value) => q.set(key, value) }
    q
  }

  def addQueryToSolr(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    val req = ctx.req
    val qParam = req.getQueryString("q").getOrElse("")

    q.setParam(CommonParams.QT, req.getQueryString("qt").orNull)

    val searchField = ctx.config.searchFields.getOrElse(req.getQueryString("search_field").orNull, null)
    if(searchField != null) {
      q.setParam(CommonParams.QT, searchField.qt)
      if(searchField.solrParameters != null) {
        searchField.solrParameters.foreach { case (key, value) => q.setParam(key, value) }
      }
    }

    if(searchField != null && searchField.solrLocalParameters.nonEmpty) {
      val local_params = searchField.solrLocalParameters.map { case (key, value) =>
        key + "=" + solrParamQuote(value, "'")
      }.mkString(" ")

      q.setQuery(s"{!$local_params}${qParam}")

      if(q.get("spellcheck.q") == null) {
        q.setParam("spellcheck.q", qParam)
      }
    }
    // there's code here to deal with q being a Hash, but I don't see how that can be...
    /*
      elsif blacklight_params[:q].is_a? Hash
        q = blacklight_params[:q]
        solr_parameters[:q] = if q.values.any?(&:blank?)
          # if any field parameters are empty, exclude _all_ results
          "{!lucene}NOT *:*"
        else
          "{!lucene}" + q.map do |field, values|
            "#{field}:(#{Array(values).map { |x| solr_param_quote(x) }.join(" OR ")})"
          end.join(" AND ")
        end
        solr_parameters[:spellcheck] = 'false'
     */
    else if(qParam != null) {
      q.setQuery(qParam)
    }
    q
  }

  def addFacetFqToSolr(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    val req = ctx.req
    val qParam = req.getQueryString("fq").orNull

    // fq shouldn't exist yet in SolrQuery. no need to do this?
    /*
      if solr_parameters[:fq].is_a? String
        solr_parameters[:fq] = [solr_parameters[:fq]]
      end
     */
    for(facetField <- ctx.searchState.getFacetFields) {
      for(value <- ctx.searchState.getFacetValues(facetField)) {
        if(value.nonEmpty) {
          q.addFilterQuery(facetValueToFqString(ctx, facetField.fieldname, value))
        }
      }
    }
    q
  }

  def addFacettingToSolr(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    for((field_name, facet) <- facetFieldsToIncludeInRequest(ctx)) {
      q.setFacet(true)

      if(facet.pivot.nonEmpty) {
        q.addFacetPivotField(withExLocalParam(facet.ex.orNull, facet.pivot.mkString(",")))
      } else if(facet.query.nonEmpty) {
        for(facetQuery <- facet.query) {
          q.addFacetQuery(withExLocalParam(facet.ex.orNull, facetQuery.fq()))
        }
      } else {
        q.addFacetField(withExLocalParam(facet.ex.orNull, facet.fieldname))
      }

      if(facet.sort != null) {
        q.setParam(s"f.${facet.fieldname}.facet.sort", facet.sort)
      }

      if(facet.solrParams.nonEmpty) {
        for((k, v) <- facet.solrParams) {
          q.setParam(s"f.${facet.fieldname}.${k}", v)
        }
      }

      val limit = facetLimitWithPagination(ctx, field_name)
      if (limit != -1) {
        q.setParam(s"f.${facet.fieldname}.facet.limit", limit.toString)
      }
    }
    q
  }

  def addSolrFieldsToQuery(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    val filteredShow = ctx.config.showFields.filter(pair => shouldAddFieldToRequest(ctx, pair._2))
    for((field_name, field) <- filteredShow) {
      if(field.solrParams.nonEmpty) {
        for((k,v) <- field.solrParams) {
          q.setParam(s"f.${field.fieldname}.${k}", v)
        }
      }
    }

    val filteredIndex = ctx.config.indexFields.filter(pair => shouldAddFieldToRequest(ctx, pair._2))
    for((field_name, field) <- filteredIndex) {
      if(field.highlight) {
        q.setHighlight(true)
        q.addHighlightField(field_name)
      }
      if(field.solrParams.nonEmpty) {
        for((k,v) <- field.solrParams) {
          q.setParam(s"f.${field.fieldname}.${k}", v)
        }
      }
    }
    q
  }

  def addPagingToSolr(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    q.setRows(rows(ctx))
    val startValue = start(ctx)
    if(startValue != 0) {
      q.setStart(startValue)
    }
    q
  }

  def addSortingToSolr(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    val sortField = sort(ctx)
    if(sortField != null) {
      q.setParam("sort", sortField)
    }
    q
  }

  def addFacetPagingToSolr(ctx: SearchBuilderContext, q: SolrQuery): SolrQuery = {
    if(ctx.browseFacet != null) {
      val facet = ctx.browseFacet
      val facetField = ctx.config.getFacetField(facet)
      withExLocalParam(facetField.ex.orNull, facet)

      // remove any existing facet fields and replace with the one we're browsing
      q.getFacetFields.foreach(fieldname => q.removeFacetField(fieldname))
      q.addFacetField(facet)

      val limit = facetField.moreLimit

      val requestKeys = facetPaginationRequestKeys(ctx)

      val page = ctx.req.getQueryString(requestKeys("page")).getOrElse("1").toInt
      val offset = (page - 1) * limit

      val sort = ctx.req.getQueryString(requestKeys("sort")).orNull
      val prefix = ctx.req.getQueryString(requestKeys("prefix")).orNull

      q.set(s"f.${facet}.facet.limit", limit + 1)
      q.set(s"f.${facet}.facet.offset", offset)
      if(sort != null) {
        q.set(s"f.${facet}.facet.sort", sort)
      }
      if(prefix != null) {
        q.set(s"f.${facet}.facet.prefix", prefix)
      }
      q.setRows(0)
    }
    q
  }

  private

  def facetFieldsToIncludeInRequest(ctx: SearchBuilderContext): Map[String, FacetFieldConfig] = {
    val config = ctx.config
    config.facetFields.filter(_._2.includeInRequest || config.addFacetFieldsToSolrRequest)
  }

  def convertToTermValue(s: String) = {
    // TODO: blacklight uses this to handle dates
    s
  }

  def facetValueToFqString(ctx: SearchBuilderContext, facetField: String, value: String): String= {
    val field = ctx.config.facetFields.getOrElse(facetField, null)

    val solr_field = if (field != null) {
      field.fieldname
    } else {
      facetField
    }

    val local_params = if (field != null && field.tag.isDefined) {
      s"tag=${field.tag.get}"
    } else {
      ""

    }
    val prefix = if (local_params.nonEmpty) s"{!${local_params}}" else ""

    if (field != null && field.query.nonEmpty) {
      field.getFacetQueryItem(value).map(_.fq()).getOrElse("-*:*")
    } else if (value.startsWith("range")) {
      // TODO: handle range values
      // "#{prefix}#{solr_field}:[#{value.first} TO #{value.last}]"
      return ""
    } else {
      return s"{!term f=${solr_field}${if(local_params.nonEmpty) " " + local_params else ""}}${convertToTermValue(value)}"
    }
  }

  def solrParamQuote(s: String, quote: String): String = {
    // TODO
    s
  }

  def withExLocalParam(ex: String, value: String): String =
    if(ex != null) {
      s"{!ex=${ex}}${value}"
    } else {
      value
    }

  def facetLimitFor(ctx: SearchBuilderContext, facetField: String): Int = {
    val field = ctx.config.facetFields.getOrElse(facetField, null)
    if(field != null) {
      return field.limit
    }
    return -1
  }

  def facetLimitWithPagination(ctx: SearchBuilderContext, fieldName: String) = {
    val limit = facetLimitFor(ctx, fieldName)
    if(limit > 0) {
      limit + 1
    } else {
      limit
    }
  }

  def shouldAddFieldToRequest(ctx: SearchBuilderContext, field: ShowFieldConfig): Boolean =
    field.includeInRequest || ctx.config.addFieldConfigurationToSolrRequest

  def shouldAddFieldToRequest(ctx: SearchBuilderContext, field: IndexFieldConfig): Boolean =
    field.includeInRequest || ctx.config.addFieldConfigurationToSolrRequest

  // this differs from Blacklight: we don't keep any state
  def rows(ctx: SearchBuilderContext): Int = {
    val req = ctx.req
    val reqValue = List[String]("rows", "per_page").map(req.getQueryString(_).getOrElse("")).find(_.nonEmpty).getOrElse("")
    val value = if(reqValue.nonEmpty) reqValue.toInt else ctx.config.defaultPerPage
    List[Int](value, ctx.config.maxPerPage).min
  }

  def start(ctx: SearchBuilderContext): Int = {
    val s = (page(ctx) - 1) * rows(ctx)
    if(s > 0) s else 0
  }

  def page(ctx: SearchBuilderContext): Int =
    ctx.req.getQueryString("page").getOrElse("1").toInt

  // returns null if we can't determine any sort order
  def sort(ctx: SearchBuilderContext): String = {
    val sortParam = ctx.req.getQueryString("sort").getOrElse("")
    val sortField = if (sortParam.nonEmpty) {
      ctx.config.sortFieldsOrdered.find(_.fieldname == sortParam).orNull
    } else {
      ctx.config.defaultSortField
    }

    val field = if (sortField != null) {
      sortField.fieldname
    } else {
      sortParam
    }

    if(field != null && field.nonEmpty) field else null
  }

  def facetPaginationRequestKeys(ctx: SearchBuilderContext): StringMap = ctx.config.facetPaginationRequestKeys

}
