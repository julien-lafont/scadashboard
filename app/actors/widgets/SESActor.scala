package actors.widgets

import play.api.Application
import play.api.libs.json.{Reads, Json}

import akka.actor.{ActorLogging, Actor, Props, ActorRef}

import actors.HubActor.Update
import actors.{EventBus, WidgetFactory}
import models.{SESNotification, SNSEvent}


object SESActor  extends WidgetFactory {
  override def props(hub: ActorRef, id: String, config: C)(implicit app: Application) = Props(new SESActor(hub, id, config))
  override type C = Unit
  override val configReader = Reads.pure(())
}

/**
 * SimpleEmailService actor receive push notifications send throug Amazon SNS
 */
class SESActor(hub: ActorRef, id: String, config: Unit)(implicit app: Application) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    val eventBus = app.injector.instanceOf(classOf[EventBus]) // Fixme: Inject by constructor
    eventBus.subscribe(self, classOf[SNSEvent[SESNotification]])
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
