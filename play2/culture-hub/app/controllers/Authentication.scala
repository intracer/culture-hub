package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.templates.groovy.GroovyTemplates
import core.ThemeAware
import play.api.libs.Crypto
import play.libs.Time
import play.api.i18n.Messages
import models.User
import util.MissingLibs

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Authentication extends Controller with GroovyTemplates with ThemeAware {

  val USERNAME = "userName"
  val REMEMBER_COOKIE = "rememberme"
  val AT_KEY = "___AT" // authenticity token

  case class Auth(userName: String, password: String)

  val loginForm = Form(
    tuple(
      "userName" -> nonEmptyText,
      "password" -> nonEmptyText,
      "remember" -> boolean
    ) verifying(Messages("authentication.error"), result => result match {
      case (u, p, r) => User.connect(u, p)
    }))

  def login = Themed {
    Action {
      implicit request =>
        if(session.get("userName").isDefined) {

          // TODO MIGRATION onAuthenticated

          Redirect(controllers.routes.Application.index)
        } else {
          Ok(Template('loginForm -> loginForm))
        }
    }
  }

  def logout = Action {
    Redirect(routes.Authentication.login).withNewSession.flashing(
      "success" -> Messages("authentication.logout")
    ).discardingCookies(REMEMBER_COOKIE)
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => BadRequest(Template("/Authentication/login.html", 'loginForm -> formWithErrors)),
        user => {
          val action = (request.session.get("uri") match {
            case Some(uri) => Redirect(uri)
            case None => Redirect(controllers.routes.Application.index)
          }).withSession("userName" -> user._1, AT_KEY -> authenticityToken)

          if (user._3) {
            action.withCookies(Cookie(
              name = REMEMBER_COOKIE,
              value = Crypto.sign(user._1) + "-" + user._1,
              maxAge = Time.parseDuration("30d")
            ))
          } else {
            action
          }
        }
      )
  }

  private def authenticityToken = Crypto.sign(MissingLibs.UUID)

}


object AccessControl {

  val ORGANIZATIONS = "organizations"
  val GROUPS = "groups"

}