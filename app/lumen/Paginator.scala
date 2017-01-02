package lumen

import lumen.response.Response
import play.api.mvc.{AnyContent, Request}

/**
  * Pagination interface for the items found in a Response object
  */
class Paginator(val config: Config, val req: Request[AnyContent], val response: Response) {

  val searchState = new SearchState(config, req.queryString)

  val totalResults: Int =
    response.solrResponse.getResults.getNumFound.toInt

  val start: Int = response.solrResponse.getResults.getStart.toInt + 1

  val size: Int = response.solrResponse.getResults.size()

  val end: Int = start + size - 1

  val showPreviousLink: Boolean = start > 1

  val showNextLink: Boolean = end < totalResults

  val currentPage: Int = {
    val page = searchState.getParamValue("page")
    if (page != null) page.toInt else 1
  }

  val nextPage: Int = currentPage + 1

  val previousPage: Int = currentPage - 1

  val previousPageLink: String =
    pageLink(previousPage)

  val nextPageLink: String =
    pageLink(nextPage)

  def pageLink(page: Int) = {
    // TODO: catalog url
    "/catalog?" + Util.queryString(searchState.setParam("page", page.toString).params)
  }

  val perPage: Int = {
    val _perPage = searchState.getParamValue("per_page")
    if(_perPage != null) _perPage.toInt else config.defaultPerPage
  }

  val needsPagination: Boolean = totalResults > perPage

  val totalPages: Int =
    (totalResults / perPage) + (if(totalResults % perPage != 0) 1 else 0)

  /**
    * This is modeled after the "kaminari" ruby gem
    * @return List of Ranges for left outer window, main window, and right outer window.
    *
    */
  def windows(windowSize: Int, outerWindowSize: Int) = {
    val empty = Range(0,0)

    // calc theoretical bounds of left and right windows
    val leftWindowHigh = if(outerWindowSize <= totalPages) outerWindowSize else totalPages
    val rightWindowLow = if(totalPages - outerWindowSize >= 1) totalPages - outerWindowSize else 1

    // main window
    val possibleHigh = currentPage + windowSize
    val cappedHigh = if(possibleHigh <= totalPages) possibleHigh else totalPages
    val possibleLow = currentPage - windowSize
    val cappedLow = if(possibleLow > 0) possibleLow else 1

    // do left/right windows overlap with main window?
    val leftWindowOverlaps = leftWindowHigh >= cappedLow
    val rightWindowOverlaps = rightWindowLow <= cappedHigh

    // if there are overlaps, adjust all windows as necessary
    val high = if(rightWindowOverlaps) totalPages else cappedHigh
    val low = if(leftWindowOverlaps) 1 else cappedLow
    val leftWindow = if(leftWindowOverlaps) empty else Range.inclusive(1, leftWindowHigh)
    val rightWindow = if(rightWindowOverlaps) empty else Range.inclusive(rightWindowLow, totalPages)
    val window = Range.inclusive(low.toInt, high.toInt)

    List(leftWindow, window, rightWindow)
  }

}
