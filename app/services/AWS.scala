package services

import scala.concurrent.Future
import javax.inject.{Inject, Singleton}

import play.api.Logger

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.ec2.AmazonEC2AsyncClient
import com.amazonaws.services.ec2.model.{DescribeInstanceStatusRequest, DescribeInstanceStatusResult, DescribeInstancesResult, DescribeInstancesRequest}
import com.github.dwhjames.awswrap.cloudwatch.AmazonCloudWatchScalaClient

import models.configs.AWSConfig

@Singleton
class AWS @Inject()(config: AWSConfig) {

  private val defaultCredentialChain = new DefaultAWSCredentialsProviderChain()

  val cloudWatchClient: AmazonCloudWatchScalaClient = {
    val asyncClient = new AmazonCloudWatchAsyncClient(defaultCredentialChain)
    asyncClient.configureRegion(config.region)

    Logger.info("[AWS-CLW] Connected with account: " + defaultCredentialChain.getCredentials.getAWSAccessKeyId)
    new AmazonCloudWatchScalaClient(asyncClient)
  }

  val ec2Client = {
    // No scala wrapper available
    val asyncClient = new AmazonEC2AsyncClient(defaultCredentialChain)
    asyncClient.configureRegion(config.region)

    Logger.info("[AWS-EC2] Connected with account: " + defaultCredentialChain.getCredentials.getAWSAccessKeyId)
    new AmazonEC2ScalaClient(asyncClient)
  }
}

/**
 * A lightweight wrapper for [[http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2AsyncClient.html AmazonEC2AsyncClient]].
 *
 * @constructor construct a wrapper client from an Amazon async client.
 * @param client
  *     the underlying [[http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2AsyncClient.html AmazonEC2AsyncClient]].
 * @see [[http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2AsyncClient.html AmazonEC2AsyncClient]]
 */
class AmazonEC2ScalaClient(val client: AmazonEC2AsyncClient) extends CustomAwsWrap {

  /**
   * @see [[http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2AsyncClient.html#describeInstances(com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest) AWS Java SDK]]
   */
  def describeInstances(describeInstancesRequest: DescribeInstancesRequest): Future[DescribeInstancesResult] = {
    wrapAsyncMethod(client.describeInstancesAsync, describeInstancesRequest)
  }

  /**
   * @see [[http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2AsyncClient.html#describeInstanceStatus(com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest) AWS Java SDK]]
   */
  def describeInstanceStatus(describeInstanceStatusRequest: DescribeInstanceStatusRequest): Future[DescribeInstanceStatusResult] = {
    wrapAsyncMethod(client.describeInstanceStatusAsync, describeInstanceStatusRequest)
  }

}

/**
 * Extracted from [[https://github.com/dwhjames/aws-wrap/blob/master/src/main/scala/package.scala]]
 */
trait CustomAwsWrap {
  import scala.concurrent.{Future, Promise}
  import java.util.concurrent.{Future => JFuture}

  import com.amazonaws.AmazonWebServiceRequest
  import com.amazonaws.handlers.AsyncHandler

  private def promiseToAsyncHandler[Request <: AmazonWebServiceRequest, Result](p: Promise[Result]) =
    new AsyncHandler[Request, Result] {
      override def onError(exception: Exception): Unit = { p.failure(exception); () }
      override def onSuccess(request: Request, result: Result): Unit = { p.success(result); () }
    }

  @inline
  def wrapAsyncMethod[Request <: AmazonWebServiceRequest, Result](
    f:       (Request, AsyncHandler[Request, Result]) => JFuture[Result],
    request: Request
    ): Future[Result] = {
    val p = Promise[Result]()
    f(request, promiseToAsyncHandler(p))
    p.future
  }

}
