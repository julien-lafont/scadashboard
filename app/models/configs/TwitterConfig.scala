package models.configs

import com.typesafe.config.Config

case class TwitterConfig(consumerKey: String, consumerSecret: String)

object TwitterConfig {

  def apply(config: Config): TwitterConfig = {
    val conf = config.getConfig("widgets.twitter")
    TwitterConfig(conf.getString("consumerKey"), conf.getString("consumerSecret"))
  }

}
