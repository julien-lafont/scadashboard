package models

import play.api.libs.json.{JsError, JsSuccess, JsString, Reads}
import org.joda.time.DateTime

trait BasicReadsWritesJson {

  /**
   * Readers for ISO-8601 as joda DateTime
   */
  val isoDateTimeReads: Reads[DateTime] = Reads {
    case JsString(date) => JsSuccess(DateTime.now())
    case _ => JsError("error.expected.isoDateTime")
  }
}
