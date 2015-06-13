package actors.widgets

import scala.collection.JavaConverters._
import scala.concurrent.duration._

import play.api.libs.json.{JsValue, Json}
import com.amazonaws.services.cloudwatch.model.{Dimension, GetMetricStatisticsRequest, Statistic}
import org.joda.time.DateTime
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.Update
import utils.AWS

object CloudWatchActor {
  def props(hub: ActorRef, name: String, config: JsValue) = Props(new CloudWatchActor(hub, name, config))
  private case object Tick
}

class CloudWatchActor(hub: ActorRef, name: String, config: JsValue) extends Actor with ActorLogging {
  import CloudWatchActor._

  val delay = (config \ "delay").asOpt[Long].getOrElse(30l)
  val namespace = (config \ "namespace").as[String]
  val metric = (config \ "metric").as[String]
  val instanceId = (config \ "instanceId").as[String]
  val period = (config \ "period").as[Int]
  val since = (config \ "since").as[Int] // hours

  val request = new GetMetricStatisticsRequest()
    .withNamespace(namespace)
    .withMetricName(metric)
    .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
    .withPeriod(period)
    .withStatistics(Statistic.Average)

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(0.seconds, delay.seconds, self, Tick)

  override def postStop(): Unit = {
    tickTask.cancel()
  }

  override def receive = {
    case Tick =>
      val currentRequest = request
        .withStartTime(DateTime.now().minusHours(since).toDate)
        .withEndTime(DateTime.now().toDate)

      AWS.cloudWatchClient.getMetricStatistics(currentRequest).map { result =>
        val json = Json.toJson(result.getDatapoints.asScala.map { datapoint =>
          datapoint.getTimestamp.getTime.toString -> BigDecimal(datapoint.getAverage)
        }.toMap)
        hub ! Update(name, json)
      }.recover {
        case ex => log.error(ex, "Cannot retrieve cloudwatch metrics")
      }
  }

}
