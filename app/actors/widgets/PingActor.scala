package actors.widgets

import scala.concurrent.duration._

import play.api.Application
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.WS

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import actors.HubActor.Update

object PingActor {
  def props(hub: ActorRef, name: String, config: JsValue)(implicit app: Application) = Props(new PingActor(hub, name, config))
  private case object Tick
}

class PingActor(hub: ActorRef, name: String, config: JsValue)(implicit app: Application) extends Actor with ActorLogging {
  import PingActor._

  val url = (config \ "url").as[String]
  val interval = (config \ "interval").asOpt[Long].getOrElse(10l)

  val query = WS.url(url).withRequestTimeout(interval * 1000l).withFollowRedirects(true)

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(0.seconds, interval.seconds, self, Tick)

  override def postStop(): Unit = {
    tickTask.cancel()
  }

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
