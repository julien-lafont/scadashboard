package actors

import play.api.Application
import play.api.libs.json.{Json, JsError, Reads, JsValue}

import actors.widgets.CodeShipActor
import akka.actor.{ActorContext, Props, ActorRef}

trait WidgetFactory {
  type C // Configuration object
  def props(hub: ActorRef, name: String, config: C)(implicit app: Application): Props
  def configReader: Reads[C]

  def init(hub: ActorRef, name: String, config: JsValue)(implicit app: Application): Either[JsValue, Props] = {
    configReader.reads(config).fold(
      errors => Left(JsError.toJson(errors)),
      config => Right(props(hub, name, config))
    )
  }
}

object WidgetFactory {

  def initialize(widget: String, hub: ActorRef, name: String, config: JsValue)(implicit app: Application, context: ActorContext): Either[JsValue, ActorRef] = {
    val widgetInitialization = widget match {
      case "codeship" => CodeShipActor.init(hub, name, config)
      case _ => Left(Json.obj("message" -> s"cannot found widget $widget"))
    }

    widgetInitialization.right.map(context.actorOf)
  }
}
