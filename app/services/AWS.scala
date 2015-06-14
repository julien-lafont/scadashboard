package services

import scala.util.Try

import play.api.{Application, Logger}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.github.dwhjames.awswrap.cloudwatch.AmazonCloudWatchScalaClient

class AWS(config: AWSConfig) {

  val cloudWatchClient: AmazonCloudWatchScalaClient = {
    val defaultCredentialChain = new DefaultAWSCredentialsProviderChain()
    Logger.info("[AWS-CLW] Connected with account: " + defaultCredentialChain.getCredentials.getAWSAccessKeyId)

    val asyncClient = new AmazonCloudWatchAsyncClient(defaultCredentialChain)
    asyncClient.configureRegion(config.region)

    new AmazonCloudWatchScalaClient(asyncClient)
  }
}

case class AWSConfig(region: Regions)

object AWSConfig {
  def apply(implicit app: Application): AWSConfig = {
    val conf = app.configuration.getConfig("widgets.aws").getOrElse(throw app.configuration.globalError("Cannot load AWS configuration from [widgets.aws]"))
    val region = conf.getString("region").flatMap(r => Try(Regions.fromName(r)).toOption).getOrElse(throw app.configuration.globalError("Invalid widgets.aws.region"))
    AWSConfig(region)
  }
}
