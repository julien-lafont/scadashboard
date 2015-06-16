package actors.widgets

import scala.util.{Failure, Success}

import play.api.Application
import play.api.libs.json._
import akka.actor._

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.CodeShipActor.CodeShipConfig
import services.Services

object CodeShipActor extends WidgetFactory {
  override type C = CodeShipConfig
  override val configReader = Json.reads[C]
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new CodeShipActor(hub, id, config, services))
  protected case class CodeShipConfig(projectId: String, branch: Option[String], interval: Option[Long])
}

class CodeShipActor(hub: ActorRef, id: String, config: CodeShipConfig, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  override def receive = {
    case Tick =>
      services.codeship.query(config.projectId, config.branch).get().onComplete {
        case Success(response) =>
          hub ! Update(id, response.json)
        case Failure(ex) =>
          log.error(ex, "Cannot retrieve Codeship project status")
          hub ! Error("Cannot retrieve Codeship project status")
      }
  }

}
