package utils

import play.api.Logger

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.github.dwhjames.awswrap.cloudwatch.AmazonCloudWatchScalaClient

object AWS {

  val cloudWatchClient: AmazonCloudWatchScalaClient = {
    val defaultCredentialChain = new DefaultAWSCredentialsProviderChain()
    Logger.info("[AWS-SES] Connected with account: " + defaultCredentialChain.getCredentials.getAWSAccessKeyId)

    val asyncClient = new AmazonCloudWatchAsyncClient(defaultCredentialChain)
    asyncClient.configureRegion(Regions.EU_WEST_1)

    new AmazonCloudWatchScalaClient(asyncClient)
  }
}
