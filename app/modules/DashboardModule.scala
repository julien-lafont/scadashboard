package modules

import play.api.libs.concurrent.AkkaGuiceSupport
import com.google.inject.Provides
import net.codingwell.scalaguice.ScalaModule

import actors.EventBus

class DashboardModule extends ScalaModule with AkkaGuiceSupport {

  override def configure() = {

  }

  @Provides
  def providesEventBus() = EventBus.apply()
}
