package actors.widgets

import play.api.Application
import play.api.libs.json.{Json, Reads}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.Update
import actors.WidgetFactory
import models.{SESNotification, SNSEvent}
import services.Services

object SESActor  extends WidgetFactory {
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new SESActor(hub, id, config, services))
  override type C = Unit
  override val configReader = Reads.pure(())
}

/**
 * SimpleEmailService actor receives push notifications send through Amazon SNS
 */
class SESActor(hub: ActorRef, id: String, config: Unit, services: Services)(implicit app: Application) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    services.eventBus.subscribe(self, classOf[SNSEvent[SESNotification]])
  }

  override def receive = {
    case SNSEvent(messageId, topicArn, message: SESNotification, timestamp) =>
      hub ! Update(id, Json.obj(
        "source" -> message.source,
        "destination" -> message.destinations.head,
        "timestamp" -> timestamp,
        "notificationType" -> message.notificationType,
        "delivered" -> (message.notificationType == "Delivery")
      ))
  }
}
