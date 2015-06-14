package models.configs

import scala.util.Try

import com.amazonaws.regions.Regions
import com.typesafe.config.Config

case class AWSConfig(region: Regions)

object AWSConfig {
  def apply(config: Config): AWSConfig = {
    val conf = config.getConfig("widgets.aws")
    val region = Try(Regions.fromName(conf.getString("region"))).getOrElse(throw new RuntimeException("Invalid widgets.aws.region"))
    AWSConfig(region)
  }
}

