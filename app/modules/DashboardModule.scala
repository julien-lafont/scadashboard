package modules

import play.api.Application
import play.api.libs.concurrent.AkkaGuiceSupport
import com.google.inject.{Singleton, Provides}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule

import actors.EventBus
import models.configs.{TwitterConfig, CodeshipConfig, GithubConfig, AWSConfig}

class DashboardModule extends ScalaModule with AkkaGuiceSupport {

  lazy val eventBus = new EventBus()

  override def configure() = {
    bind[EventBus].toInstance(eventBus)
  }

  @Provides
  @Singleton
  def providesAppConfig(app: Application): Config = app.configuration.underlying

  @Provides
  @Singleton
  def providesAwsConfig(config: Config): AWSConfig = AWSConfig(config)

  @Provides
  @Singleton
  def providesGithubConfig(config: Config): GithubConfig = GithubConfig(config)

  @Provides
  @Singleton
  def providesCodeshipConfig(config: Config): CodeshipConfig = CodeshipConfig(config)

  @Provides
  @Singleton
  def providesTwitterConfig(config: Config): TwitterConfig = TwitterConfig(config)
}
