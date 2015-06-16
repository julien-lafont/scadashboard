package services

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

import play.api.Application
import play.api.libs.json.JsValue
import play.api.libs.ws.{WS, WSRequest}

import models.configs.GithubConfig

@Singleton
class Github @Inject()(
  config: GithubConfig,
  implicit private val app: Application,
  implicit private val ec: ExecutionContext) {

  private val base = "https://api.github.com"

  def getRepository(owner: String, repository: String): Future[Either[String, JsValue]] = {
    url(s"/repos/$owner/$repository").get().map { response =>
      response.status match {
        case 200 => Right(response.json)
        case _ => Left(s"Unable to call repository.get($owner:$repository)")
      }
    }
  }

  def url(route: String): WSRequest = {
    WS.url(base + route)
      .withRequestTimeout(5000l)
      .withHeaders("Authorization" -> s"token ${config.accessToken}")
  }

}
