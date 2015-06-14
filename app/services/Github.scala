package services

import javax.inject.{Inject, Singleton}

import play.api.Application
import play.api.libs.ws.{WS, WSRequest}

import models.configs.GithubConfig

@Singleton
class Github @Inject()(config: GithubConfig, implicit val app: Application) {

  private val base = "https://api.github.com"

  def url(route: String): WSRequest = {
    WS.url(base + route)
      .withRequestTimeout(5000l)
      .withHeaders("Authorization" -> s"token ${config.accessToken}")
  }

}
