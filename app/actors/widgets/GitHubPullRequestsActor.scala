package actors.widgets

import scala.concurrent.Future

import play.api.libs.json._
import play.api.Application

import akka.actor._
import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.GitHubPullRequestsActor.GitHubPRConfig
import services.Github

object GitHubPullRequestsActor extends WidgetFactory {
  override type C = GitHubPRConfig
  override val configReader = Json.reads[GitHubPRConfig]
  override def props(hub: ActorRef, id: String, config: C)(implicit app: Application) = Props(new GitHubPullRequestsActor(hub, id, config))
  protected case class GitHubPRConfig(organization: String, repository: Option[String], interval: Option[Long])
}

class GitHubPullRequestsActor(hub: ActorRef, id: String, config: GitHubPRConfig)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  val github = app.injector.instanceOf(classOf[Github])

  val queryRepositories = github.url(s"/orgs/${config.organization}/repos")
  def queryPullRequests(repo: String) = github.url(s"/repos/${config.organization}/$repo/pulls")

  override def receive = {
    case Tick =>
      queryRepositories.get().foreach { response =>
        val repositoryNames = (response.json \\ "name").map(_.as[String])

        log.debug(s"Repositories found: $repositoryNames")

        config.repository match {
          // Load PR from just one repository
          case Some(repo) if repositoryNames.contains(repo) =>
            fetchPRInformation(repo).foreach { prs =>
              hub ! Update(id, JsArray(prs))
            }

          // Load PR from all repositories
          case None =>
            Future.sequence(repositoryNames.map(fetchPRInformation)).foreach { prs =>
              hub ! Update(id, Json.toJson(prs.flatten))
            }

          // Repository not found
          case Some(repo) =>
            hub ! Error(s"Cannot found repository $repo on organization ${config.organization}")
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
