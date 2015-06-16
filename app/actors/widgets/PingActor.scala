package actors.widgets

import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WS
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.Update
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.PingActor.PingConfig
import services.Services

object PingActor extends WidgetFactory {
  override type C = PingConfig
  override val configReader = Json.reads[C]
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new PingActor(hub, id, config, services))
  protected case class PingConfig(url: String, fetchContent: Option[Boolean], interval: Option[Long])
}

class PingActor(hub: ActorRef, id: String, config: PingConfig, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  val url = config.url
  override val interval = config.interval.getOrElse(10l)

  val fetchContent = config.fetchContent.getOrElse(true)
  val query = WS.url(url).withRequestTimeout(interval * 5000l).withFollowRedirects(true)

  override def receive = {
    case Tick =>
      val start = System.currentTimeMillis()
      val method = if (fetchContent) "GET" else "HEAD"

      query.execute(method).onComplete { response =>
        val ping = System.currentTimeMillis() - start
        val success = response.map(r => r.status >= 200 && r.status < 300).getOrElse(false)
        hub ! Update(id, Json.obj(
          "url" -> url,
          "success" -> success,
          "ping" -> ping))
      }
  }
}
