package actors.widgets

import scala.concurrent.duration._
import scala.collection.JavaConverters._

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import com.amazonaws.services.cloudwatch.model.{Statistic, Dimension, GetMetricStatisticsRequest}
import org.joda.time.DateTime
import play.api.libs.json.{Json, JsValue}

import utils.AWS
import actors.HubActor.Forward

object CloudWatchActor {
  def props(out: ActorRef, config: JsValue) = Props(new CloudWatchActor(out, config))
  private case object Tick
}

class CloudWatchActor(hub: ActorRef, config: JsValue) extends Actor with ActorLogging {
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
        hub ! Forward(json)
      }.recover {
        case ex => log.error(ex, "Cannot retrieve cloudwatch metrics")
      }
  }

}
