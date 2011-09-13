package controllers

/**
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Portal extends DelvingController {

  import views.User._

  // todo change this with the real portal skins etc
  def index(user: String): AnyRef = {
    val u = getUser(user) match {
      case Right(aUser) => aUser
      case Left(error) => return error
    }
    html.index(username = u.reference.username)
  }

}