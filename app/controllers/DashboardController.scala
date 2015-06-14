package controllers

import scala.concurrent.ExecutionContext
import javax.inject._

import play.api._
import play.api.mvc._

import actors.HubActor
import models.Protocol

class DashboardController @Inject() (
  implicit val ec: ExecutionContext,
  implicit val app: Application) extends Controller {

  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  def ws = WebSocket.acceptWithActor[Protocol.InEvent, Protocol.OutEvent] { request => out =>
    HubActor.props(out)
  }

}





