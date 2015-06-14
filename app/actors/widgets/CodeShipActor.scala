package actors.widgets

import scala.util.{Failure, Success}

import play.api.libs.json._
import play.api.Application

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.CodeShipActor.CodeShipConfig
import akka.actor._
import services.Codeship

object CodeShipActor extends WidgetFactory {
  override type C = CodeShipConfig
  override val configReader = Json.reads[CodeShipConfig]
  override def props(hub: ActorRef, id: String, config: C)(implicit app: Application) = Props(new CodeShipActor(hub, id, config))
  protected case class CodeShipConfig(projectId: String, branch: Option[String], interval: Option[Long])
}

class CodeShipActor(hub: ActorRef, id: String, config: CodeShipConfig)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  val codeship = app.injector.instanceOf(classOf[Codeship])

  override def receive = {
    case Tick =>
      codeship.query(config.projectId, config.branch).get().onComplete {
        case Success(response) =>
          hub ! Update(id, response.json)
        case Failure(ex) =>
          log.error(ex, "Cannot retrieve Codeship project status")
          hub ! Error("Cannot retrieve Codeship project status")
      }
  }

}
