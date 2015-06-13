package actors.widgets

import scala.concurrent.duration._
import scala.util.{Failure, Success}

import play.api.libs.json._
import play.api.Application
import play.api.libs.ws.WS

import actors.HubActor.Update
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.CodeShipActor.CodeShipConfig
import akka.actor._

object CodeShipActor extends WidgetFactory {
  override type C = CodeShipConfig
  override val configReader = Json.reads[CodeShipConfig]
  override def props(hub: ActorRef, name: String, config: CodeShipActor.C)(implicit app: Application) = Props(new CodeShipActor(hub, name, config))
  protected case class CodeShipConfig(projectId: String, branch: Option[String], interval: Option[Long])
}

class CodeShipActor(hub: ActorRef, name: String, config: CodeShipConfig)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  val apikey = app.configuration.getString("widgets.codeship.apikey").getOrElse(throw new RuntimeException("Cannot load widgets.codeship.apikey"))
  val projectId = config.projectId
  val branch = config.branch.getOrElse("master")

  val url = s"https://codeship.com/api/v1/projects/$projectId.json?api_key=$apikey&branch=$branch"
  val query = WS.url(url).withRequestTimeout(5000l)

  log.info(s"Codeship widget launched with $config")

  override def receive = {
    case Tick =>
      query.get().onComplete {
        case Success(response) => hub ! Update(name, response.json)
        case Failure(ex) => log.error(ex, "Cannot retrieve Codeship project status")
      }
  }

}
