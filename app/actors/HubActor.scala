package actors

import play.api.Application
import play.api.libs.json.{Json, JsObject, JsValue}

import actors.widgets._
import akka.actor._

object HubActor {
  def props(out: ActorRef)(implicit app: Application) = Props(new HubActor(out))

  case class Forward(json: JsValue)
  case object Stop
}

class HubActor(out: ActorRef)(implicit app: Application) extends Actor with ActorLogging {
  import HubActor._

  // Save active actors for the current user
  val actors = collection.mutable.Map[String, ActorRef]()
  // Id generator
  val index = new java.util.concurrent.atomic.AtomicLong(1)

  override def receive = {
    case Forward(json) =>
      log.info(s">> $json")
      out ! json

    case json: JsValue =>
      log.info(s"<< $json")
      val action = (json \ "action").asOpt[String]

      action match {
        case Some("start") =>
          val widget = (json \ "widget").as[String]
          val config = (json \ "config").asOpt[JsObject].getOrElse(Json.obj())
          val id = index.getAndIncrement()
          val name = s"$widget:$id"
          log.info(s"Start widget '$name'")

          val actor = widget match {
            case "ping" => context.actorOf(PingActor.props(self, config), name)
            case "codeship" => context.actorOf(CodeShipActor.props(self, config), name)
            case "cloudwatch" => context.actorOf(CloudWatchActor.props(self, config), name)
          }

          actors.put(name, actor)
          self ! Forward(Json.obj("action" -> "started", "name" -> name))

        case Some("stop") =>
          val name = (json \ "name").as[String]
          actors.get(name) match {
            case Some(actor) =>
              actor ! PoisonPill
              actors.remove(name)
              self ! Forward(Json.obj("action" -> "stopped", "name" -> name))
          }

        case Some("stop-all") =>
          actors.foreach { case (name, actor) =>
            actor ! PoisonPill
            actors.remove(name)
            self ! Forward(Json.obj("action" -> "stopped", "name" -> name))
          }

        case Some("status") =>
          self ! Forward(Json.toJson(actors.keys))

        case other =>
          log.warning("Action not handled !")
      }
  }

}
