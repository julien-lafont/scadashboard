package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

import org.joda.time.DateTime

import actors.EventBus.EventMessage

/**
 * Event sent by Amazon SNS
 *
 * @param messageId
 * @param topicArn
 * @param message
 * @param timestamp
 */
case class SNSEvent[T](messageId: String, topicArn: String, message: T, timestamp: DateTime) extends EventMessage

/**
 * SimpleEmailService notification
 *
 * @param notificationType Delivery/Bounce
 * @param source
 * @param destinations
 */
case class SESNotification(notificationType: String, source: String, destinations: Seq[String])

object SNSEvent extends BasicReadsWritesJson {

  implicit val sesNotificationReader: Reads[SESNotification] = {
    (
      (__ \ "notificationType").read[String] and
      (__ \ "mail" \ "source").read[String] and
      (__ \ "mail" \ "destination").read[Seq[String]]
    )(SESNotification.apply _)
  }

  implicit def snsEventReader[T](implicit readSubMessage: Reads[T]): Reads[SNSEvent[T]] = (
    (__ \ "MessageId").read[String] and
    (__ \ "TopicArn").read[String] and
    (__ \ "Message").read[String].map(Json.parse).andThen(readSubMessage) and
    (__ \ "Timestamp").read[DateTime](isoDateTimeReads)
  )(SNSEvent.apply[T] _)
}
