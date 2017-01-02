package lumen

/**
  * The search that is in the user's current browser session.
  */
class SearchSession(val reqCtx: RequestContext) {

  setupNextAndPreviousDocuments()

  def setupNextAndPreviousDocuments() = {
    // if(reqCtx.req.session.get("counter").nonEmpty &&
  }

/*

  def setup_next_and_previous_documents
    if search_session['counter'] and current_search_session
      index = search_session['counter'].to_i - 1
      response, documents = get_previous_and_next_documents_for_search index, ActiveSupport::HashWithIndifferentAccess.new(current_search_session.query_params)

      search_session['total'] = response.total
      @search_context_response = response
      @previous_document = documents.first
      @next_document = documents.last
    end
  rescue Blacklight::Exceptions::InvalidRequest => e
    logger.warn "Unable to setup next and previous documents: #{e}"
  end

 */

}
