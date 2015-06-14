package services

import javax.inject.{Singleton, Inject}

import play.api.Application
import play.api.libs.ws.{WS, WSRequest}

import models.configs.CodeshipConfig

@Singleton
class Codeship @Inject() (config: CodeshipConfig, implicit val app: Application) {

  private val base = "https://codeship.com/api/v1/"

  def query(projectId: String, branch: Option[String]): WSRequest = {
    WS.url(base + s"/projects/$projectId.json")
      .withQueryString(
        "api_key" -> config.apiKey,
        "branch" -> branch.getOrElse("master"))
      .withRequestTimeout(10000l)

  }
}
