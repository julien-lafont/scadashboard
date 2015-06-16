package actors.widgets

import play.api.Application
import play.api.libs.json._
import akka.actor._

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.GitHubRepositoryActor.GitHubRepositoryConfig
import services.Services

object GitHubRepositoryActor extends WidgetFactory {
  override type C = GitHubRepositoryConfig
  override val configReader = Json.reads[C]
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new GitHubRepositoryActor(hub, id, config, services))
  protected case class GitHubRepositoryConfig(owner: String, repository: String, interval: Option[Long])
}

class GitHubRepositoryActor(hub: ActorRef, id: String, config: GitHubRepositoryConfig, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  override def receive = {
    case Tick =>
      services.github.getRepository(config.owner, config.repository).map {
        case Left(error) => hub ! Error(error)
        case Right(json) => hub ! Update(id, json)
      }
  }

}
