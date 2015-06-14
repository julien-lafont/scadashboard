package models.configs

import com.typesafe.config.Config

case class CodeshipConfig(apiKey: String)

object CodeshipConfig {

  def apply(config: Config): CodeshipConfig = {
    val conf = config.getConfig("widgets.codeship")
    CodeshipConfig(conf.getString("apikey"))
  }

}
