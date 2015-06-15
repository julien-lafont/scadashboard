package actors.widgets

import scala.util._

import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.{WSAuthScheme, WS}

import actors.widgets.TwitterUserActor.TwitterUserConfig
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import models.configs.TwitterConfig

object TwitterUserActor  extends WidgetFactory {
  override type C = TwitterUserConfig
  override val configReader = Json.reads[C]
  override def props(hub: ActorRef, id: String, config: C)(implicit app: Application) = Props(new TwitterUserActor(hub, id, config))
  protected case class TwitterUserConfig(screenName: String, interval: Option[Long])
}

class TwitterUserActor(hub: ActorRef, id: String, config: TwitterUserConfig)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)
  assert(interval >= 5, "You cannot refresh this twitter api more than 180 times for 15mn (so not more than every 5s)")

  val twitterConfig = app.injector.instanceOf(classOf[TwitterConfig])

  val getToken = WS.url("https://api.twitter.com/oauth2/token")
    .withAuth(twitterConfig.consumerKey, twitterConfig.consumerSecret, WSAuthScheme.BASIC)
    .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
  val getTokenData = "grant_type=client_credentials"

  def fetchUserInfo(token: String) = WS.url(s"https://api.twitter.com/1.1/users/show.json?screen_name=${config.screenName}")
    .withHeaders("Authorization" -> s"Bearer $token")

  // TODO: Remove duplication on error handling
  override def receive = {
    case Tick =>
      getToken.post(getTokenData).onComplete {
        case Success(response) =>
          response.status match {
            case 200 =>
              val accessToken = (response.json \ "access_token").as[String]
              fetchUserInfo(accessToken).get().onComplete {
                case Success(userResponse) =>
                  response.status match {
                    case 200 => hub ! Update(id, userResponse.json)
                    case _ => reportError(s"Unable to retrieve twitter access_token from credentials.\nStatus: ${userResponse.status}\nBody: ${userResponse.body}")
                  }
                case Failure(ex) => reportError("Cannot send the request for /users/show", Some(ex))
              }
            case _ => reportError(s"Unable to retrieve twitter access_token from credentials.\nStatus: ${response.status}\nBody: ${response.body}")
          }
        case Failure(ex) => reportError("Cannot send the request for access_token", Some(ex))
      }
  }

  private def reportError(msg: String, throwable: Option[Throwable] = None) = {
    throwable match {
      case Some(ex) => log.error(ex, msg)
      case _ => log.error(msg)
    }
    hub ! Error(msg)
  }

}
