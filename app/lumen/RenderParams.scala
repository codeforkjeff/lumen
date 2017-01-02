package lumen

/**
  * Classes can have methods that create RenderParams for
  * code in templates to render. This avoids classes calling
  * render() directly from outside templates.
  *
  * @param templatePath
  * @param params
  */
class RenderParams(val templatePath: String, val params: Map[String, Any]) {

}
