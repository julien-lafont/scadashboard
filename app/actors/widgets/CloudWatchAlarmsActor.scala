package actors.widgets

import scala.collection.JavaConverters._

import play.api.Application
import play.api.libs.json.Json
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.CloudWatchAlarmsActor.CloudWatchAlarmsConfig
import services.Services

object CloudWatchAlarmsActor extends WidgetFactory {
  override type C = CloudWatchAlarmsConfig
  override val configReader = Json.reads[CloudWatchAlarmsConfig]
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new CloudWatchAlarmsActor(hub, id, config, services))
  protected case class CloudWatchAlarmsConfig(all: Option[Boolean], alarmNames: Option[Seq[String]], interval: Option[Long])
}

class CloudWatchAlarmsActor(hub: ActorRef, id: String, config: CloudWatchAlarmsConfig, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(30l)
  val alarmNames = config.alarmNames

  val request = new DescribeAlarmsRequest()

  if (!config.all.getOrElse(false)) request.withStateValue("ALARM")
  config.alarmNames.foreach { names =>
    request.withAlarmNames(names: _*)
  }

  override def receive = {
    case Tick =>
      services.aws.cloudWatchClient.describeAlarms(request).map { result =>
        val json = Json.toJson(result.getMetricAlarms.asScala.map { alarm =>
          Json.obj(
            "namespace" -> alarm.getNamespace,
            "name" -> alarm.getAlarmName,
            "description" -> alarm.getAlarmDescription,
            "updatedAt" -> alarm.getStateUpdatedTimestamp,
            "state" -> alarm.getStateValue,
            "metricName" -> alarm.getMetricName,
            "reason" -> alarm.getStateReason
          )
        })

        hub ! Update(id, json)
      }.recover {
        case ex =>
          log.error(ex, "Cannot retrieve cloudwatch alarms")
          hub ! Error(s"Cannot retrieve cloudwatch alarms")
      }
  }

}
