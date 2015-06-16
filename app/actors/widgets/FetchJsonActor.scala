package actors.widgets

import scala.util.{Failure, Success}

import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.WS
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.FetchJsonActor.FetchJsonConfig
import services.Services

object FetchJsonActor extends WidgetFactory {
  override type C = FetchJsonConfig
  override val configReader = Json.reads[C]
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new FetchJsonActor(hub, id, config, services))
  protected case class FetchJsonConfig(url: String, method: Option[String], interval: Option[Long])
}

class FetchJsonActor(hub: ActorRef, id: String, config: FetchJsonConfig, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(10l)

  val query = WS
    .url(config.url)
    .withRequestTimeout(interval * 1000)
    .withFollowRedirects(true)
    .withHeaders("Accept" -> "application/json")

  override def receive = {
    case Tick =>
      query.execute(config.method.map(_.toUpperCase).getOrElse("GET")).onComplete {
        case Success(response) => hub ! Update(id, response.json)
        case Failure(ex) =>
          log.error(ex, s"Cannot fetch ${config.url}")
          hub ! Error(s"Cannot fetch ${config.url}")
      }
  }
}
