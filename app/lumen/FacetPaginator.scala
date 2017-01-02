package lumen

import org.apache.solr.client.solrj.response.FacetField

/**
  * Handles filtering of facets in Solr response, "more" functionality
  * @param sort 'count' or 'index', solr tokens for facet value sorting, default 'count'.
  */
class FacetPaginator(val itemsOriginal: List[FacetField.Count],
                     val offset: Int = 0,
                     val limit: Int = -1,
                     val sort: String = "count",
                     val prefix: Option[String] = None) {

  // Note: Blacklight had a request_keys hash which we've moved to Config

  // The number of records solr gave us when we asked for limit + 1 records at the current offset
  def totalCount = itemsOriginal.size

  def items = itemsForLimit()

  private def itemsForLimit(): List[FacetField.Count] = {
    if(limit == -1) itemsOriginal else itemsOriginal.take(limit)
  }

  def currentPage: Int =
    if(limit == -1 || limit == 0) 1 else offset / limit + 1

  def isLastPage: Boolean =
    limit == -1 || totalCount <= limit

  def isFirstPage: Boolean =
    currentPage == 1

  def totalPages = -1

  def prevPage =
    if(!isFirstPage) currentPage - 1 else 1

  def nextPage =
    if(!isLastPage) currentPage + 1 else currentPage

  /*
      # Pass in a desired solr facet solr key ('count' or 'index', see
      # http://wiki.apache.org/solr/SimpleFacetParameters#facet.limit
      # under facet.sort ), and your current request params.
      # Get back params suitable to passing to an ActionHelper method for
      # creating a url, to resort by that method.
      # @param [String] sort_method
      # @param [HashWithIndifferentAccess] params
      def params_for_resort_url(sort_method, params = {})
        # When resorting, we've got to reset the offset to start at beginning,
        # no way to make it make sense otherwise.
        resort_params = params.merge(request_keys[:sort] => sort_method, request_keys[:page] => nil)

        if sort_method == 'count'
          resort_params.except!(request_keys[:prefix])
        end

        resort_params
      end

      def as_json(_ = nil)
        { 'items' => items.as_json, 'limit' => limit, 'offset' => offset, 'sort' => sort }
      end

   */
}
