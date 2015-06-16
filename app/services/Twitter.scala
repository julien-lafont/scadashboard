package services

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

import play.api.cache.Cache
import play.api.libs.json.JsValue
import play.api.libs.ws.{WS, WSAuthScheme}
import play.api.{Application, Logger}

import models.configs.TwitterConfig
import utils.rich._

class Twitter @Inject()(
  twitterConfig: TwitterConfig,
  implicit private val app: Application,
  implicit private val ec: ExecutionContext) {

  private val url = "https://api.twitter.com"

  def loadUserInformation(username: String): Future[Either[String, JsValue]] = {
    fetchTokenFromCache().flatMapRight { token =>
      WS.url(s"$url/1.1/users/show.json?screen_name=$username")
        .withHeaders("Authorization" -> s"Bearer $token")
        .get()
        .map { response =>
          response.status match {
            case 200 => Right(response.json)
            case _ =>
              Logger.error(s"Unable to retrieve twitter profile $username with token $token.\nStatus: ${response.status}\nBody: ${response.body}")
              Left("Unable to retrieve twitter profile")
          }
      }
    }
  }

  def searchTweets(query: String, resultType: String, count: Int): Future[Either[String, JsValue]] = {
    fetchTokenFromCache().flatMapRight { token =>
      WS.url(s"$url/1.1/search/tweets.json")
        .withQueryString(
          "q" -> query,
          "result_type" -> resultType,
          "count" -> count.toString
        )
        .withHeaders("Authorization" -> s"Bearer $token")
        .get()
        .map { response =>
          response.status match {
            case 200 => Right(response.json)
            case _ =>
              Logger.error(s"Unable to search tweets with query $query and token $token.\nStatus: ${response.status}\nBody: ${response.body}")
              Left("Unable to search tweets")
          }
        }
    }
  }

  private def fetchToken(): Future[Either[String, String]] = {
    WS.url(s"$url/oauth2/token")
      .withAuth(twitterConfig.consumerKey, twitterConfig.consumerSecret, WSAuthScheme.BASIC)
      .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post("grant_type=client_credentials")
      .map { response =>
        response.status match {
          case 200 => Right((response.json \ "access_token").as[String])
          case _ =>
            Logger.error(s"Unable to retrieve twitter access_token from credentials.\nStatus: ${response.status}\nBody: ${response.body}")
            Left("Unable to retrieve twitter access_token")
        }
      }
  }

  private def fetchTokenFromCache(): Future[Either[String, String]] = {
    Cache.getOrElse("twitter.token", 60*60)(fetchToken())
  }

}
