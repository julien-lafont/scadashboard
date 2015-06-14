package controllers

import play.api.Logger
import play.api.mvc._
import com.google.inject.Inject

import actors.EventBus
import models.{SESNotification, SNSEvent}
import models.SNSEvent._

class AmazonSNSController @Inject()(eventBus: EventBus) extends Controller {

  def ses = Action(parse.json[SNSEvent[SESNotification]]) { req =>
    Logger.debug(s"SES notification received: ${req.body}")

    eventBus.publish(req.body)
    NoContent
  }
}
