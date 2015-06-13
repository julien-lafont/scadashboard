package actors.widgets

import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor._
import play.api.Application
import play.api.libs.json.JsValue
import play.api.libs.ws.WS

import actors.HubActor.Forward

object CodeShipActor {
  def props(out: ActorRef, config: JsValue)(implicit app: Application) = Props(new CodeShipActor(out, config))
  private case object Tick
}

class CodeShipActor(hub: ActorRef, config: JsValue)(implicit app: Application) extends Actor with ActorLogging {
  import CodeShipActor._

  val apikey = (config \ "apikey").as[String]
  val projectId = (config \ "projectId").as[String]
  val branch = (config \ "branch").asOpt[String].getOrElse("master")
  val delay = (config \ "delay").asOpt[Long].getOrElse(30l)

  val url = s"https://codeship.com/api/v1/projects/$projectId.json?api_key=$apikey&branch=$branch"

  val query = WS.url(url).withRequestTimeout(5000l)

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(0.seconds, delay.seconds, self, Tick)

  override def postStop(): Unit = {
    tickTask.cancel()
  }

  override def receive = {
    case Tick =>
      query.get().onComplete {
        case Success(response) => hub ! Forward(response.json)
        case Failure(ex) => log.error(ex, "Cannot retrieve Codeship project status")
      }

  }
}
