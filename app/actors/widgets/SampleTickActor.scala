/**
 * Sample widget
 *
 * You can start this widget by sending this json through the websocket
 * ```
 *  { "action": "start", "data": { "widget": "SampleTick", "id": "uniqueId", "config": { "message": "Hello", "interval": 1 } } }
 * ```
 */
package actors.widgets

import play.api.Application
import play.api.libs.json.Json
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.Update
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.SampleTickActor.SampleTickConfig
import services.Services

/**
 * You must define for each widget a companion class extending WidgetFactory
 * This object will be user to create new actors on the fly
 */
object SampleTickActor extends WidgetFactory {

  /**
   * Each widget can have a specific configuration, defined in the JSON by the `config` object
   */
  protected case class SampleTickConfig(message: String, interval: Option[Long])

  /**
   * Just say here how is name your config case class
   */
  override type C = SampleTickConfig

  /**
   * Just say here how to reads your configuration object from JSON
   * If you don't want to make specific validation, just write `Json.reads[C]`
   */
  override val configReader = Json.reads[C]

  /**
   * The props allows the actorSystem to start new actors
   * Just call Props(new MyActor(hub, id, config, service)
   */
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) =
    Props(new SampleTickActor(hub, id, config, services))
}

/**
 * The actor which will be created to handle your widget.
 *
 * This actor is a `TickActor`: at a regular interval, he will receive a `Tick` command.
 *
 * @param hub The hub is the actor linked to the user.
 *            Send an `Update` command to forward new data to the user
 *            Send an `Error` command to forward an error to the user
 * @param id  Unique identifier of this actor (handled by the system)
 * @param config Specific configuration of the user
 * @param services Business services facade
 */
class SampleTickActor(hub: ActorRef, id: String, config: SampleTickConfig, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {

  /**
   * Interval define the time between two updates
   */
  override val interval = config.interval.getOrElse(1l)

  override def receive = {
    case Tick =>

      /**
       * Here you can define the behavior of your widget.
       * Send message to the hub like that:
       */
      val json = Json.obj("foo" -> (config.message + "world !"))
      hub ! Update(id, json)
  }
}
