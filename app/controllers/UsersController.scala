package controllers

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import models.{User, Users}

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits._

class UsersController(userServices: Users)(cc: ControllerComponents) extends AbstractController(cc) {

  implicit val userWrites: Writes[User] = Json.writes[User]
  implicit val userReads: Reads[User] = (
      Reads.pure(0L) and
      (JsPath \ "name").read[String] and
      (JsPath \ "isActive").read[Boolean]
    )(User.apply _)

  def get(id: Long) = Action.async { request =>
    userServices.find(id) match {
      case None => async {NotFound}
      case Some(user) => async {Ok(Json.toJson(user))}
    }
  }

  def create = Action(parse.json) { request =>
    Json.fromJson[User](request.body).fold(
      invalid => BadRequest,
      user => {
        val userCreated = userServices.create(user)
        Created.withHeaders(LOCATION -> s"/users/${userCreated.id}")
      }
    )
  }

  def delete(id: Long) = Action { request =>
    userServices.find(id) match {
      case None => NotFound
      case Some(user) =>
        userServices.delete(user)
        NoContent
    }
  }

  def update(id: Long) = Action(parse.json) { request =>
    Json.fromJson[User](request.body).fold(
      invalid => BadRequest,
      user => {
        userServices.update(user.copy(id = id))
        NoContent
      }
    )
  }
}
