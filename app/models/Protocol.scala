package models

import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

object Protocol {
  case class InEvent(action: String, data: JsObject)
  case class OutEvent(event: String, data: JsValue)

  implicit val inEventFormat = Json.format[InEvent]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[InEvent]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]
}
