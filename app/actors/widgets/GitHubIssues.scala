package actors.widgets

import scala.concurrent.Future
import scala.util.{Failure, Success}

import play.api.libs.json._
import play.api.Application
import play.api.libs.ws.WS

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.GitHubIssuesActor.GitHubIssuesConfig
import akka.actor._

object GitHubIssuesActor extends WidgetFactory {
  override type C = GitHubIssuesConfig
  override val configReader = Json.reads[GitHubIssuesConfig]
  override def props(hub: ActorRef, id: String, config: C)(implicit app: Application) = Props(new GitHubIssuesActor(hub, id, config))
  protected case class GitHubIssuesConfig(organization: String, repository: Option[String], interval: Option[Long])
}

class GitHubIssuesActor(hub: ActorRef, id: String, config: GitHubIssuesConfig)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  val accesstoken = app.configuration.getString("widgets.github.accesstoken").getOrElse(throw app.configuration.globalError("Cannot load Github accesstoken [widgets.github.accesstoken]"))
  val organization = config.organization
  val repository = config.repository

  val urlRepositories = s"https://api.github.com/orgs/$organization/repos?access_token=$accesstoken"
  def urlRepositoryIssues(repo: String) = s"https://api.github.com/repos/$organization/$repo/issues?access_token=$accesstoken"

  val queryRepositories = WS.url(urlRepositories).withRequestTimeout(5000l)
  def queryRepositoryIssues(repo: String) = WS.url(urlRepositoryIssues(repo)).withRequestTimeout(5000l)

  override def receive = {
    case Tick =>

      queryRepositories.get().foreach { response =>
        val repositoryNames = (response.json \\ "name").map(_.as[String])

        log.debug(s"Repositories found: $repositoryNames")

        repository match {
          // Load PR from just one repository
          case Some(repo) if repositoryNames.contains(repo) =>
            fetchIssuesInformation(repo).foreach { prs =>
              hub ! Update(id, JsArray(prs))
            }

          // Load PR from all repositories
          case None =>
            Future.sequence(repositoryNames.map(fetchIssuesInformation)).foreach { prs =>
              hub ! Update(id, Json.toJson(prs.flatten))
            }

          // Repository not found
          case Some(repo) =>
            hub ! Error(s"Cannot found repository $repo on organization $organization")
        }
      }
  }

  /**
   * Extract only useful data from Issues:list API
   */
  private def fetchIssuesInformation(repo: String): Future[Seq[JsObject]] = {
    queryRepositoryIssues(repo).get().map { response =>
      val pullRequests = response.json.as[JsArray]

      log.debug(s"Load PR for repo $repo: ${pullRequests.value.size}")

      pullRequests.value.flatMap { json =>
        val isAPullRequest = (json \ "pull_request").asOpt[JsObject].isDefined

        // Pull-Requests are also seens as an issue by Github. Ignore these one
        if (isAPullRequest) None
        else {
          val title = (json \ "title").as[String]
          val created_at = (json \ "created_at").as[String]
          val creator = (json \ "user" \ "login").as[String]
          val avatar = (json \ "user" \ "avatar_url").as[String]
          val number = (json \ "number").as[Long]
          val labels = (json \ "labels" \\ "name").map(_.as[String])
          val url = (json \ "url").as[String]

          // Repository name is not send in the JSON, so extract it from the url
          val repo = url match {
            case extractRepositoryRegex(name) => name
            case _ => "unknown"
          }

          Some(Json.obj(
            "repository" -> repo,
            "title" -> title,
            "createdAt" -> created_at,
            "creator" -> creator,
            "avatar" -> avatar,
            "labels" -> labels,
            "number" -> number
          ))
        }
      }
    }

  }

  private val extractRepositoryRegex = ".*/([^/]*)/issues/.*".r

}
