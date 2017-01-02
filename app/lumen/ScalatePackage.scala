package lumen

import org.fusesource.scalate.support.TemplatePackage
import org.fusesource.scalate.{Binding, RenderContext, TemplateSource}

/**
  * Scalate will use this top-level package when compiling templates under lumen/ namespce.
  */
class ScalatePackage extends TemplatePackage {

  def header(source: TemplateSource, bindings: List[Binding]) = """
import lumen.ScalatePackage._
  """
}

object ScalatePackage {

  implicit class ExtendRenderContext(renderContext: RenderContext) {
    def render(renderParams: RenderParams): Unit = {
      renderContext.render(renderParams.templatePath, renderParams.params)
    }
  }

}
