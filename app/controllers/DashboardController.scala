package controllers

import scala.concurrent.ExecutionContext
import javax.inject._

import play.api._
import play.api.libs.json._
import play.api.mvc._

import actors.HubActor

class DashboardController @Inject() (
  implicit ec: ExecutionContext, app: Application) extends Controller {

  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  def ws = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    HubActor.props(out)
  }
}





