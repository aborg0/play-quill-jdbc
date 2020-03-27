package controllers

import play.api.mvc.{AbstractController, Action, ControllerComponents, Results}

class HomeController(controllerComponents: ControllerComponents) extends AbstractController(controllerComponents){
  import Results._


  def index = Action {request =>
    Ok(views.html.index())
  }
}
