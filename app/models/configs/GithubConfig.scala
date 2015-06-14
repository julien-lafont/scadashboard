package models.configs

import com.typesafe.config.Config

case class GithubConfig(accessToken: String)

object GithubConfig {

  def apply(config: Config): GithubConfig = {
    val conf = config.getConfig("widgets.github")
    GithubConfig(conf.getString("accesstoken"))
  }

}
