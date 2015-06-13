package actors.helpers

import scala.concurrent.duration._
import akka.actor.{Cancellable, Actor}

/**
 * A TickActor receives a `Tick` command each `interval` seconds
 */
trait TickActor extends Actor {

  // Interval (in seconds) between two ticks
  def interval: Long

  // Event sent to `self` on each interval
  protected case object Tick

  // Save the tisk cancellable task to stop the scheduller when the actor stops
  protected var tickTask: Cancellable = _

  // Start the scheduller when the actor is ready
  override def preStart(): Unit = {
    tickTask = context.system.scheduler.schedule(0.seconds, interval.seconds, self, Tick)(context.dispatcher)
  }

  // Stop the scheduler when the actor is destroyed
  override def postStop(): Unit = {
    tickTask.cancel()
  }

}
