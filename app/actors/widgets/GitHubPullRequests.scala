package actors.widgets

import scala.concurrent.Future

import play.api.libs.json._
import play.api.Application
import play.api.libs.ws.WS

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.GitHubPullRequestsActor.GitHubPRConfig
import akka.actor._

object GitHubPullRequestsActor extends WidgetFactory {
  override type C = GitHubPRConfig
  override val configReader = Json.reads[GitHubPRConfig]
  override def props(hub: ActorRef, name: String, config: C)(implicit app: Application) = Props(new GitHubPullRequestsActor(hub, name, config))
  protected case class GitHubPRConfig(organization: String, repository: Option[String], interval: Option[Long])
}

class GitHubPullRequestsActor(hub: ActorRef, name: String, config: GitHubPRConfig)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  val accesstoken = app.configuration.getString("widgets.github.accesstoken").getOrElse(throw app.configuration.globalError("Cannot load Github accesstoken [widgets.github.accesstoken]"))
  val organization = config.organization
  val repository = config.repository

  val urlRepositories = s"https://api.github.com/orgs/$organization/repos?access_token=$accesstoken"
  def urlPullRequests(repo: String) = s"https://api.github.com/repos/$organization/$repo/pulls?access_token=$accesstoken"

  val queryRepositories = WS.url(urlRepositories).withRequestTimeout(5000l)
  def queryPullRequests(repo: String) = WS.url(urlPullRequests(repo)).withRequestTimeout(5000l)

  override def receive = {
    case Tick =>
      queryRepositories.get().foreach { response =>
        val repositoryNames = (response.json \\ "name").map(_.as[String])

        log.debug(s"Repositories found: $repositoryNames")

        repository match {
          // Load PR from just one repository
          case Some(repo) if repositoryNames.contains(repo) =>
            fetchPRInformation(repo).foreach { prs =>
              hub ! Update(name, JsArray(prs))
            }

          // Load PR from all repositories
          case None =>
            Future.sequence(repositoryNames.map(fetchPRInformation)).foreach { prs =>
              hub ! Update(name, Json.toJson(prs.flatten))
            }

          // Repository not found
          case Some(repo) =>
            hub ! Error(s"Cannot found repository $repo on organization $organization")
        }
      }
  }

  /**
   * Extract only useful data from PR:list api
   */
  private def fetchPRInformation(repo: String): Future[Seq[JsObject]] = {
    queryPullRequests(repo).get().map { response =>
      val pullRequests = response.json.as[JsArray]

      log.debug(s"Load PR for repo $repo: ${pullRequests.value.size}")

      pullRequests.value.map { json =>
        val title = (json \ "title").as[String]
        val created_at = (json \ "created_at").as[String]
        val creator = (json \ "user" \ "login").as[String]
        val avatar = (json \ "user" \ "avatar_url").as[String]

        Json.obj(
          "repository" -> repo,
          "title" -> title,
          "createdAt" -> created_at,
          "creator" -> creator,
          "avatar" -> avatar
        )
      }
    }
  }

}
