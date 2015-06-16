package actors.widgets

import scala.util._

import play.api.Application
import play.api.libs.json.Json
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.TwitterSearchActor.TwitterSearchConfig
import services.Services

object TwitterSearchActor extends WidgetFactory {
  override type C = TwitterSearchConfig
  override val configReader = Json.reads[C]
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new TwitterSearchActor(hub, id, config, services))
  protected case class TwitterSearchConfig(query: String, resultType: Option[String], count: Option[Int], interval: Option[Long])
}

class TwitterSearchActor(hub: ActorRef, id: String, config: TwitterSearchConfig, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(60l)

  override def receive = {
    case Tick =>
      services.twitter
        .searchTweets(config.query, config.resultType.getOrElse("recent"), config.count.getOrElse(5))
        .map {
          case Left(error) => hub ! Error(error)
          case Right(data) => hub ! Update(id, data)
        }
  }

}
