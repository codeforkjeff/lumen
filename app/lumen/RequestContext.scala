package lumen

import com.github.tototoshi.play2.scalate.Scalate
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

/**
  * Encapsulates a request and any per-request objects.
  * Includes "global" vars like the LumenApp too, which it
  * probably shouldn't, but it makes life awfully convenient.
  *
  * I'm not totally sure this class is a good idea.
  *
  * @param app
  * @param req
  * @param scalate
  * @param messagesApi
  */
class RequestContext(val app: LumenApp,
                     val req: Request[AnyContent],
                     val scalate: Scalate,
                     val messagesApi: MessagesApi)
  extends I18nSupport {

  val messages: Messages = request2Messages(req)

  val catalogPresenter: CatalogPresenter =
    app.config.catalogPresenterFactory(this, scalate.engine)()

  def applicationName = messages("lumen.application_name")

}
