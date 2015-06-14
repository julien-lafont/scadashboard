package services

import javax.inject.{Inject, Singleton}

import play.api.Logger
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.github.dwhjames.awswrap.cloudwatch.AmazonCloudWatchScalaClient

import models.configs.AWSConfig

@Singleton
class AWS @Inject()(config: AWSConfig) {

  val cloudWatchClient: AmazonCloudWatchScalaClient = {
    val defaultCredentialChain = new DefaultAWSCredentialsProviderChain()
    Logger.info("[AWS-CLW] Connected with account: " + defaultCredentialChain.getCredentials.getAWSAccessKeyId)

    val asyncClient = new AmazonCloudWatchAsyncClient(defaultCredentialChain)
    asyncClient.configureRegion(config.region)

    new AmazonCloudWatchScalaClient(asyncClient)
  }
}

