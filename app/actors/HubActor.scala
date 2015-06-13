package actors

import play.api.Application
import play.api.libs.json.{Json, JsObject, JsValue}

import actors.widgets._
import akka.actor._
import models.Protocol.{OutEvent, InEvent}

object HubActor {
  def props(out: ActorRef)(implicit app: Application) = Props(new HubActor(out))

  case class Forward(event: String, data: JsValue)
  case class Update(name: String, data: JsValue)
  case object Stop
}

class HubActor(out: ActorRef)(implicit app: Application) extends Actor with ActorLogging {
  import HubActor._

  // Save active actors for the current user
  val actors = collection.mutable.Map[String, ActorRef]()
  // Id generator
  val index = new java.util.concurrent.atomic.AtomicLong(1)

  val widgets = Map(
    "ping" -> PingActor.props _,
    "codeship" -> CodeShipActor.props _,
    "cloudwatch" -> CloudWatchActor.props _
  )

  override def receive = {
    case Forward(event, json) =>
      log.info(s">> $json")
      out ! OutEvent(event, json)

    case Update(name, json) =>
      log.info(s">> $json")
      out ! OutEvent("update", Json.obj(name -> json))

    case InEvent(action: String, data: JsValue) =>
      log.info(s"<< ($action) $data")

      action match {
        case "start" =>
          val widget = (data \ "widget").as[String]
          val config = (data \ "config").asOpt[JsObject].getOrElse(Json.obj())

          val id = index.getAndIncrement()
          val name = s"$widget:$id"

          log.info(s"Start widget '$name'")

          widgets.get(widget) match {
            case Some(props) =>
              val actor = context.actorOf(props(self, name, config), name)
              actors.put(name, actor)
              self ! Forward("started", Json.obj("name" -> name))
            case None =>
              log.warning(s"Cannot start unknown widget $widget")
          }

        case "stop" =>
          data.asOpt[String].foreach { name =>
            actors.get(name).foreach(actor =>
              stopActor(name, actor))
          }

        case "stop-all" =>
          actors.foreach { case (name, actor) =>
            stopActor(name, actor)
          }

        case "status" =>
          self ! Forward("status", Json.toJson(actors.keys))

        case other =>
          log.warning("Action not handled !")
      }
  }

  private def stopActor(name: String, actor: ActorRef) = {
    actor ! PoisonPill
    actors.remove(name)
    self ! Forward("stopped", Json.obj("name" -> name))
  }

}
