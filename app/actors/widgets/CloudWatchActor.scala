package actors.widgets

import scala.collection.JavaConverters._

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.Application
import play.api.libs.json.Json
import com.amazonaws.services.cloudwatch.model.{Dimension, GetMetricStatisticsRequest, Statistic}
import org.joda.time.DateTime

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.CloudWatchActor.CloudWatchConfig
import utils.AWS

object CloudWatchActor extends WidgetFactory {
  override type C = CloudWatchConfig
  override val configReader = Json.reads[CloudWatchConfig]
  override def props(hub: ActorRef, name: String, config: C)(implicit app: Application) = Props(new CloudWatchActor(hub, name, config))
  protected case class CloudWatchConfig(namespace: String, metric: String, instanceId: String, period: Int, since: Int, interval: Option[Long])
}

class CloudWatchActor(hub: ActorRef, name: String, config: CloudWatchConfig) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(30l)
  val namespace = config.namespace
  val metric = config.metric
  val instanceId = config.instanceId
  val period = config.period
  val since = config.since

  val request = new GetMetricStatisticsRequest()
    .withNamespace(namespace)
    .withMetricName(metric)
    .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
    .withPeriod(period)
    .withStatistics(Statistic.Average)

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
        case ex =>
          log.error(ex, "Cannot retrieve cloudwatch metrics")
          hub ! Error(s"Cannot retrieve cloudwatch metrics $namespace:$metric")
      }
  }

}
