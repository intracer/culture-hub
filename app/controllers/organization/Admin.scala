package controllers.organization

import controllers.DelvingController
import play.mvc.results.Result
import play.mvc.Before
import extensions.JJson
import models.{User, Organization}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Admin extends DelvingController with OrganizationSecured {

  @Before(priority = 2) def checkOwner(): Result = {
    val orgId = params.get("orgId")
    if (orgId == null || orgId.isEmpty) Error("How did you even get here?")
    if (!Organization.isOwner(connectedUser)) return Forbidden(&("user.secured.noAccess"))
    Continue
  }

  def index(orgId: String): Result = {
    val org = Organization.findByOrgId(orgId).getOrElse(return NotFound("Could not find organization " + orgId))
    val membersAsTokens = JJson.generate(org.users.map(m => Map("id" -> m, "name" -> m)))
    Template('members -> membersAsTokens)
  }

  /**
   * Add to organization
   */
  def addUser(orgId: String, id: String): Result = {
    User.findByUsername(id).getOrElse(return Error(&("organizations.admin.userNotFound", id)))
    val success = Organization.addUser(orgId, id)
    // TODO logging
    if (success) Ok else Error
  }

  /**
   * Remove from organization
   */
  def removeUser(orgId: String, id: String): Result = {
    User.findByUsername(id).getOrElse(return Error(&("organizations.admin.userNotFound", id)))
    val success = Organization.removeUser(orgId, id)
    // TODO logging
    if (success) Ok else Error
  }

}