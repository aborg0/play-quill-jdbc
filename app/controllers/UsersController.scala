package controllers

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, MessagesAbstractController, MessagesControllerComponents, MessagesRequest, Request, Result}
import models.{User, Users}
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import views.CreateUser

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class UsersController(userServices: Users)(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  implicit val userWrites: Writes[User] = Json.writes[User]
  implicit val userReads: Reads[User] = (
      Reads.pure(0L) and
      (JsPath \ "name").read[String] and
      (JsPath \ "isActive").read[Boolean]
    )(User.apply _)

  val userForm = Form(
    mapping(
      "name" -> text
    )(CreateUser.apply)(CreateUser.unapply)
  )

  def get(id: Long) = Action.async { request =>
    userServices.find(id) match {
      case None => async {NotFound}
      case Some(user) => async {Ok(Json.toJson(user))}
    }
  }

  def allActive = Action.async {implicit request: Request[AnyContent] =>
    Future {
      userServices.allActiveUsers.take(10)
    }.map(us => Ok(views.html.Users(request.messages(messagesApi)("home.title"), us)(userForm)))
  }

  def create = Action/*(parse.json)*/ { implicit request =>
    val json = request.body.asJson
    json.fold(request.body.asFormUrlEncoded.fold(BadRequest: Result){formData =>
      userForm.bindFromRequest(formData).value.fold(BadRequest: Result){create =>
        userServices.create(User(0, create.name, true))
        Redirect("/users")
      }
    })(jsValue =>
    Json.fromJson[User](jsValue).fold(
      invalid => BadRequest,
      user => {
        val userCreated = userServices.create(user)
        Created.withHeaders(LOCATION -> s"/users/${userCreated.id}")
      }
    )
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
