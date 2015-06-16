package actors

import scala.util.control.Exception.catching
import akka.actor.{ActorContext, Props, ActorRef}

import play.api.Application
import play.api.libs.json.{Json, JsError, Reads, JsValue}

import services.Services

object WidgetFactory {

  /**
   * Load a widget designed by it's WidgetFactory object
   */
  def initialize(widgetFactory: WidgetFactory)(hub: ActorRef, name: String, config: JsValue, services: Services)(implicit app: Application, context: ActorContext): Either[JsValue, ActorRef] = {
    val widgetInitialization = widgetFactory.init(hub, name, config, services)
    widgetInitialization.right.map(context.actorOf)
  }

  /**
   * Load a widget designed by it's name.
   * The widget is loaded by introspection
   */
  def initialize(widget: String)(hub: ActorRef, id: String, config: JsValue, services: Services)(implicit app: Application, context: ActorContext): Either[JsValue, ActorRef] = {
    val expectedObjectName = s"actors.widgets.${widget.take(1).toUpperCase}${widget.drop(1)}Actor"
    val widgetFactory = getCompanionClass[WidgetFactory](expectedObjectName)

    widgetFactory match {
      case Some(factory) => initialize(factory)(hub, id, config, services: Services)
      case None => Left(Json.obj("message" -> s"cannot found widget $widget (no `$expectedObjectName` object extending `WidgetFactory` found)"))
    }
  }

  private def getCompanionClass[T](name : String)(implicit man: Manifest[T]) : Option[T] =
    catching(classOf[RuntimeException]).opt(Class.forName(name + "$").getField("MODULE$").get(man.runtimeClass).asInstanceOf[T])

}

/**
 * Each widget must have an object extending WidgetFactory, which
 * allows to create new actor with different configuration
 */
trait WidgetFactory {
  // Configuration case class
  type C

  // Props for creating a new actor of this type
  def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application): Props

  // Read and parse config from JSON
  def configReader: Reads[C]

  /**
   * Try to create a new Props for this actor with the config parsed
   * Return Right(props) if the action succeed, elsewhere Left(error)
   */
  def init(hub: ActorRef, id: String, config: JsValue, services: Services)(implicit app: Application): Either[JsValue, Props] = {
    configReader.reads(config).fold(
      errors => Left(JsError.toJson(errors)),
      config => Right(props(hub, id, config, services))
    )
  }
}
