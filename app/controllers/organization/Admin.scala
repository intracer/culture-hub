package controllers.organization

import controllers.OrganizationController
import extensions.JJson
import play.api.i18n.Messages
import play.api.mvc.Action
import models._
import core.HubModule
import play.api.libs.ws.WS
import concurrent.Await
import concurrent.duration._
import com.escalatesoft.subcut.inject.BindingModule

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

class Admin(implicit val bindingModule: BindingModule) extends OrganizationController {

  def index = OrganizationAdmin {
    Action {
      implicit request =>
        val membersAsTokens = JJson.generate(HubUser.dao.listOrganizationMembers(configuration.orgId).map(m => Map("id" -> m, "name" -> m)))
        val adminsAsTokens = JJson.generate(organizationServiceLocator.byDomain.listAdmins(configuration.orgId).map(a => Map("id" -> a, "name" -> a)))
        Ok(Template(
          'members -> membersAsTokens,
          'admins -> adminsAsTokens
        ))
    }
  }

  /**
   * Add to organization
   */
  def addUser = OrganizationAdmin {
    Action {
      implicit request =>
        val id = request.body.getFirstAsString("id").get
        HubUser.dao.findByUsername(id).map { user =>
          val success = HubUser.dao.addToOrganization(id, configuration.orgId)
          // TODO logging
          if (success) Ok else Error
        }.getOrElse {
          Error(Messages("hub.CouldNotFindUser", id))
        }
    }
  }

  /**
   * Remove from organization
   */
  def removeUser = OrganizationAdmin {
    Action {
      implicit request =>
        val id = request.body.getFirstAsString("id").get
        HubUser.dao.findByUsername(id).map { user =>
          val success = HubUser.dao.removeFromOrganization(id, configuration.orgId)
          // TODO logging
          if (success) Ok else Error
        }.getOrElse {
          Error(Messages("hub.CouldNotFindUser", id))
        }
    }
  }

  def addAdmin = OrganizationAdmin {
    Action {
      implicit request =>
        val id = request.body.getFirstAsString("id").get
        HubUser.dao.findByUsername(id).map { user =>
          val success = organizationServiceLocator.byDomain.addAdmin(configuration.orgId, id)
          // TODO logging
          if (success) Ok else Error
        }.getOrElse {
          Error(Messages("hub.CouldNotFindUser", id))
        }
    }
  }

  def removeAdmin = OrganizationAdmin {
    Action {
      implicit request =>
        val id = request.body.getFirstAsString("id").get
        val success = organizationServiceLocator.byDomain.removeAdmin(configuration.orgId, id)
        if (success) Ok else Error
    }
  }

  def solrSearchProxy = OrganizationAdmin {
    Action {
      implicit request =>
        val solrQueryString: String = request.rawQueryString
        val solrServerUrl: String = String.format("%s/select?%s", configuration.solrBaseUrl, solrQueryString)

        try {
          val future = WS.url(solrServerUrl).get()
          val response = Await.result(future, 5 seconds).xml

          Ok(response)
        } catch {
          case t: Throwable => InternalServerError(t.getMessage)
        }
    }
  }

  def reProcessAll = OrganizationAdmin {
    Action {
      implicit request =>

        log.info(s"[$connectedUser${configuration.orgId}] Marking all enabled DataSets to be re-process")

        DataSet.dao.findByState(DataSetState.ENABLED).foreach { dataSet =>
          DataSet.dao.updateState(dataSet, DataSetState.QUEUED, Some(connectedUser))
        }

        Ok
    }
  }

}