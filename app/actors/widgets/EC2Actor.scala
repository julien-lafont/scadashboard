package actors.widgets

import scala.collection.JavaConverters._

import play.api.Application
import play.api.libs.json.{JsArray, Json}
import com.amazonaws.services.ec2.model.{DescribeInstancesRequest, Filter}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import actors.HubActor.{Error, Update}
import actors.WidgetFactory
import actors.helpers.TickActor
import actors.widgets.EC2Actor.EC2Config
import services.Services

object EC2Actor extends WidgetFactory {
  override type C = EC2Config
  override val configReader = Json.reads[EC2Config]
  override def props(hub: ActorRef, id: String, config: C, services: Services)(implicit app: Application) = Props(new EC2Actor(hub, id, config, services))
  protected case class EC2Config(interval: Option[Long])
}

class EC2Actor(hub: ActorRef, id: String, config: EC2Config, services: Services)(implicit app: Application) extends Actor with TickActor with ActorLogging {
  import context.dispatcher

  override val interval = config.interval.getOrElse(30l)

  val request = new DescribeInstancesRequest()
    .withFilters(new Filter().withName("instance-state-name").withValues("running"))

  override def receive = {
    case Tick =>

      services.aws.ec2Client.describeInstances(request).map { result =>
        val json = JsArray(result.getReservations.asScala.map { reservation =>
          val instance = reservation.getInstances.get(0)
          Json.obj(
            "instanceId" -> instance.getInstanceId,
            "instanceType" -> instance.getInstanceType,
            "tags" -> Json.toJson(instance.getTags.asScala.map(t => t.getKey -> t.getValue).toMap),
            "launchTime" -> instance.getLaunchTime,
            "publicIpAddress" -> instance.getPublicIpAddress,
            "privateIpAddress" -> instance.getPrivateIpAddress
          )
        })
        hub ! Update(id, json)
      }.recover {
        case ex =>
          log.error(ex, "Cannot retrieve ec2 instances")
          hub ! Error(s"Cannot retrieve ec2 instances")
      }
  }

}
