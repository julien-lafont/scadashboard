package actors.widgets

import scala.concurrent.duration._

import play.api.Application
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.WS

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import actors.HubActor.Forward

object PingActor {
  def props(out: ActorRef, config: JsValue)(implicit app: Application) = Props(new PingActor(out, config))
  private case object Ping
}

class PingActor(out: ActorRef, config: JsValue)(implicit app: Application) extends Actor with ActorLogging {
  import PingActor._

  val url = (config \ "url").as[String]
  val delay = (config \ "delay").asOpt[Long].getOrElse(10l)

  val query = WS.url(url).withRequestTimeout(delay * 1000l).withFollowRedirects(true)

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(0.seconds, delay.seconds, self, Ping)

  override def postStop(): Unit = {
    tickTask.cancel()
  }

  override def receive = {
    case Ping =>
      val start = System.currentTimeMillis()
      query.get().onComplete { response =>
        val ping = System.currentTimeMillis() - start
        val success = response.map(r => r.status >= 200 && r.status < 300).getOrElse(false)
        out ! Forward(Json.obj("url" -> url, "success" -> success, "ping" -> ping))
      }
  }
}
