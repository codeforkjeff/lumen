package lumen

import org.apache.solr.common.SolrDocument

trait DocumentAction {

  def docPresenter: DocumentPresenter

  def id: String

  def label: String

  /**
    * HTML fragment for this action, to be displayed in a menu of actions.
    * This replaces the _document_action partial.
    */
  def toolHtml: String

}

trait AjaxModalActionLink extends DocumentAction {

  override def toolHtml: String =
    s"""<a id="${id}Link" data-ajax-modal="trigger" href="${modalLink}">${label}</a>"""

  /** Link to show in a modal */
  def modalLink = s"/catalog/${docPresenter.getId}/${id}"
}

class EmailAction(override val docPresenter: DocumentPresenter)
  extends AjaxModalActionLink {

  override def id = "email"

  override def label = "Email"
}

object EmailAction {
  def apply(docPresenter: DocumentPresenter) = new EmailAction(docPresenter)
}

class BookmarkAction(override val docPresenter: DocumentPresenter)
  extends DocumentAction {

  override def id = "library_view"

  override def label = "Librarian View"

  override def toolHtml: String =
    s"""<form class="bookmark_toggle form-horizontal" data-doc-id="${docPresenter.getId}" data-present="In Bookmarks" data-absent="Bookmark" data-inprogress="Saving..." action="/bookmarks/6849454" accept-charset="UTF-8" method="post">
       |<input style="display: none;" name="utf8" value="✓" type="hidden">
       |<input style="display: none;" name="_method" value="put" type="hidden">
       |<div class="checkbox toggle_bookmark">
       |<label title="" for="toggle_bookmark_${docPresenter.getId}" class="toggle_bookmark">
       |<input id="toggle_bookmark_${docPresenter.getId}" class="toggle_bookmark" type="checkbox">
       |<span>Bookmark</span>
       |</label>
       |</div>
       |</form>
        """.stripMargin

}

object BookmarkAction {
  def apply(docPresenter: DocumentPresenter) = new BookmarkAction(docPresenter)
}
