package actors

import play.api.Application
import play.api.libs.json.{Json, JsObject, JsValue}

import actors.widgets.SESActor
import akka.actor._
import models.Protocol._
import services.Services

object HubActor {
  def props(out: ActorRef, services: Services)(implicit app: Application) = Props(new HubActor(out, services))

  case class Forward(event: String, data: JsValue)
  case class Update(id: String, data: JsValue)
  case class Error(message: String)
}

/**
 * The Hub is the actor connected to the user thought the WebSocket.
 * Each widget sends it's data to the hub, who transmits to the user
 */
class HubActor(out: ActorRef, services: Services)(implicit app: Application) extends Actor with ActorLogging {
  import HubActor._
  implicit val actorContext = context

  // Save active actors for the current user
  val actors = collection.mutable.Map[String, ActorRef]()

  override def preStart(): Unit = {
    // Launched actors at startup
    addActor("ses", context.actorOf(SESActor.props(self, "ses", (), services)))
  }

  override def receive = {
    // Forward a simple message to the user (notification, error...)
    case Forward(event, json) =>
      log.debug(s">> $json")
      out ! OutEvent(event, json)

    // Send a metric update to the client
    case Update(id, json) =>
      log.debug(s">> $json")
      out ! OutEvent("update", Json.obj(id -> json))

    // Send an error to the client
    case Error(message) =>
      out ! OutEvent("error", Json.obj("message" -> message))

    // Request sent by the client
    case InEvent(action: String, data: JsValue) =>
      log.info(s"<< ($action) $data")

      action match {
        case "start" =>
          val widget = (data \ "widget").as[String]
          val id = (data \ "id").as[String]
          val config = (data \ "config").asOpt[JsObject].getOrElse(Json.obj())

          if (actors.contains(id)) {
            self ! Error(s"This id ($id) already exists...")
          } else {

            log.info(s"Starting widget '$id'")

            WidgetFactory.initialize(widget)(self, id, config, services).fold(
              error => {
                self ! Forward("error", error)
                log.warning(s"Cannot initialize new widget $widget: $error")
              },
              actor => addActor(id, actor)
            )

          }
        case "stop" =>
          data.asOpt[String].foreach { id =>
            actors.get(id).foreach(actor =>
              stopActor(id, actor))
          }

        case "stop-all" =>
          actors.foreach { case (id, actor) =>
            stopActor(id, actor)
          }

        case "status" =>
          self ! Forward("status", Json.toJson(actors.keys))

        case other =>
          log.warning("Action not handled !")
      }
  }

  private def stopActor(id: String, actor: ActorRef) = {
    actor ! PoisonPill
    actors.remove(id)
    self ! Forward("stopped", Json.obj("id" -> id))
  }

  private def addActor(id: String, actor: ActorRef) = {
    actors.put(id, actor)
    self ! Forward("started", Json.obj("id" -> id))
  }

}
