package lumen.controllers

import com.github.tototoshi.play2.scalate.Scalate
import lumen.{Config, LumenApp, RequestContext}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Controller, Request}

/**
  * Subclasses should inject these parameters
  */
class BaseController (val messagesApi: MessagesApi,
                      val scalate: Scalate,
                      val config: Config,
                      val app: LumenApp)
  extends Controller with I18nSupport {

  def createRequestContext(implicit req: Request[AnyContent]) =
    new RequestContext(app, req, scalate, messagesApi)

  /**
    * returns a map of vars needed by default layout template
    * @param req
    * @return
    */
  def defaultLayoutMap()(implicit req: Request[AnyContent]) : Map[String, Any] = {
    Map(
      "reqCtx" -> createRequestContext
    )
  }

}
