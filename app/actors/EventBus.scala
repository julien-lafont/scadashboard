package actors

import play.api.libs.json.JsValue

import akka.event.{ActorEventBus, SubchannelClassification}
import akka.util.Subclassification

object EventBus {

  /**
   * The base event
   */
  trait EventMessage

  /**
   * Event pushed by an external service
   */
  case class ExternalEvent(id: String, payload: JsValue) extends EventMessage

}

/**
 * Basic EventBus allowing to publish messages around actors
 */
class EventBus extends ActorEventBus with SubchannelClassification {
  import EventBus._

  override type Event = EventMessage
  override type Classifier = Class[_ <: EventMessage]

  /**
   * The logic to form sub-class hierarchy
   */
  override protected implicit val subclassification = new Subclassification[Classifier] {
    def isEqual(x: Classifier, y: Classifier): Boolean = x == y
    def isSubclass(x: Classifier, y: Classifier): Boolean = y.isAssignableFrom(x)
  }

  /**
   * Publishes the given Event to the given Subscriber.
   *
   * @param event The Event to publish.
   * @param subscriber The Subscriber to which the Event should be published.
   */
  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  /**
   * Returns the Classifier associated with the given Event.
   *
   * @param event The event for which the Classifier should be returned.
   * @return The Classifier for the given Event.
   */
  override protected def classify(event: Event): Classifier = event.getClass

}
