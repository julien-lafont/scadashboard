package actors.widgets

import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WS

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.Update
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.PingActor.PingConfig

object PingActor  extends WidgetFactory {
  override type C = PingConfig
  override val configReader = Json.reads[PingConfig]
  override def props(hub: ActorRef, name: String, config: C)(implicit app: Application) = Props(new PingActor(hub, name, config))
  protected case class PingConfig(url: String, inverval: Option[Long])
}

class PingActor(hub: ActorRef, name: String, config: PingConfig)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  val url = config.url
  override val interval = config.inverval.getOrElse(10l)

  val query = WS.url(url).withRequestTimeout(interval * 1000l).withFollowRedirects(true)

  override def receive = {
    case Tick =>
      val start = System.currentTimeMillis()
      query.get().onComplete { response =>
        val ping = System.currentTimeMillis() - start
        val success = response.map(r => r.status >= 200 && r.status < 300).getOrElse(false)
        hub ! Update(name, Json.obj("url" -> url, "success" -> success, "ping" -> ping))
      }
  }
}
