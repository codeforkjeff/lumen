package lumen

// blacklight/layout_helper_behavior.rb
class Layout() {

  def showContentClasses = mainContentClasses + " show-document"

  def showSidebarClasses = sidebarClasses

  def mainContentClasses = "col-md-9 col-sm-8"

  def sidebarClasses = "col-md-3 col-sm-4"

  def containerClasses = "container"

  def parameterize(s: String): String = Util.parameterize(s)

  /*
  TODO:
     29:  def render_page_title
     44:  def render_link_rel_alternates(document=@document, options = {})
     52:  def render_opensearch_response_metadata
     */
  def renderBodyClass = extraBodyClasses.mkString(" ")

  def extraBodyClasses: Seq[String] = {
    // TODO:
    val controller_name = "catalog"
    val action_name = "index"
    List(
      "lumen-" + controller_name,
      "lumen-" + List(controller_name, action_name).mkString("-")
    )
  }

}

object Layout {
  def apply() = new Layout()
}