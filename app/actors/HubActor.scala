package actors

import play.api.Application
import play.api.libs.json.{Json, JsObject, JsValue}

import akka.actor._
import models.Protocol.{OutEvent, InEvent}

object HubActor {
  def props(out: ActorRef)(implicit app: Application) = Props(new HubActor(out))

  case class Forward(event: String, data: JsValue)
  case class Update(name: String, data: JsValue)
  case class Error(message: String)
}

/**
 * The Hub is the actor connected to the user thought the WebSocket.
 * Each widget sends it's data to the hub, who transmits to the user
 */
class HubActor(out: ActorRef)(implicit app: Application) extends Actor with ActorLogging {
  import HubActor._
  implicit val actorContext = context

  // Save active actors for the current user
  val actors = collection.mutable.Map[String, ActorRef]()

  // Id generator
  val index = new java.util.concurrent.atomic.AtomicLong(1)

  override def receive = {
    // Forward a simple message to the user (notification, error...)
    case Forward(event, json) =>
      log.debug(s">> $json")
      out ! OutEvent(event, json)

    // Send a metric update to the client
    case Update(name, json) =>
      log.debug(s">> $json")
      out ! OutEvent("update", Json.obj(name -> json))

    // Send an error to the client
    case Error(message) =>
      out ! OutEvent("error", Json.obj("message" -> message))

    // Request sent by the client
    case InEvent(action: String, data: JsValue) =>
      log.info(s"<< ($action) $data")

      action match {
        case "start" =>
          val widget = (data \ "widget").as[String]
          val config = (data \ "config").asOpt[JsObject].getOrElse(Json.obj())

          val id = index.getAndIncrement()
          val name = s"$widget:$id"

          log.info(s"Starting widget '$name'")

          WidgetFactory.initialize(widget)(self, name, config).fold(
            error => {
              self ! Forward("error", error)
              log.warning(s"Cannot initialize new widget $widget: $error")
            },
            actor => addActor(name, actor)
          )

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

  private def addActor(name: String, actor: ActorRef) = {
    actors.put(name, actor)
    self ! Forward("started", Json.obj("name" -> name))
  }

}
