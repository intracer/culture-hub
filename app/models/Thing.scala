/*
 * Copyright 2011 Delving B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import org.apache.solr.common.SolrInputDocument
import com.novus.salat.annotations.raw.Salat
import org.bson.types.ObjectId
import java.util.Date
import util.Constants._
import views.context._

@Salat
trait Base extends AnyRef with Product {

  val _id: ObjectId
  val TS_update: Date
  val user_id: ObjectId
  val userName: String

}


/**
 * A Thing
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

@Salat
trait Thing extends Base with Universal {

  val name: String
  val description: String
  val visibility: Visibility
  val deleted: Boolean
  val thumbnail_id: Option[ObjectId]
  val thumbnail_url: Option[String]
  val links: List[EmbeddedLink]

  def freeTextLinks = links.filter(_.linkType == Link.LinkType.FREETEXT)
  def placeLinks = links.filter(_.linkType == Link.LinkType.PLACE)

  def url = "/%s/%s/%s".format(userName, getType, _id)

  def getType: String
  def toSolrDocument: SolrInputDocument = getAsSolrDocument

  def flattenLinksWithIds(linkType: String, key: String) = {
    links.filter(_.linkType == linkType).filter(_.value.contains(key)).map(l => (l, new ObjectId(l.value(key)))).toList
  }

  def thumbnail(size: Int = 100): String = (thumbnail_id, thumbnail_url) match {
    case (None,  None) => getThumbnailUrl(None)
    case (Some(id), None) => getThumbnailUrl(Some(id), size)
    case (None, Some(url)) => url
  }

  // ~~~ universal accessors
  def getMongoId = _id.toString
  def getHubId = "%s_%s_%s".format(userName, getType, _id)
  def getOwnerId = userName
  def getRecordType = getType
  def getTitle = name
  def getDescription = description
  def getOwner = userName
  def getCreator = userName
  def getVisibility = visibility.value.toString
  def getUri = url
  def getLandingPage = url
  def getThumbnailUri = getThumbnailUri(180)
  def getThumbnailUri(size: Int) = thumbnail(size)
  def getMimeType = "unknown/unknown"
  def hasDigitalObject = thumbnail_id != None || thumbnail_url != None

  // ~~~ special (internal) accessors:
  def getThumbnailIdInternal: String = {
    if(thumbnail_id != None) thumbnail_id.get.toString else {
      links.find(_.linkType == Link.LinkType.THUMBNAIL).headOption match {
        case Some(e) => e.value(MDR_HUB_ID)
        case None => ""
      }
    }
  }

  protected def getAsSolrDocument: SolrInputDocument = {
    val doc = new SolrInputDocument
    doc addField (ID, _id)
    doc addField (HUB_ID, getHubId)
    doc addField (RECORD_TYPE, getType)
    doc addField (VISIBILITY, visibility.value.toString)
    doc addField (OWNER, userName)
    doc addField (CREATOR, userName)
    doc addField ("europeana_provider_single", userName) // TODO remove?
    doc addField (DESCRIPTION, description)
    doc addField (TITLE, name)
    if (thumbnail_id != None) {
      doc addField(THUMBNAIL, thumbnail_id.get)
    } else if (thumbnail_url != None) {
      doc addField(THUMBNAIL, thumbnail_url.get)
    }
    doc addField(HAS_DIGITAL_OBJECT, hasDigitalObject)
    doc
  }

}


case class Visibility(value: Int)
object Visibility {
  val PRIVATE = Visibility(0)
  val PUBLIC = Visibility(10)
  val values: Map[Int, String] = Map(PRIVATE.value -> "private", PUBLIC.value -> "public")
  def name(value: Int): String = values.get(value).getOrElse(throw new IllegalArgumentException("Illegal value %s for Visibility".format(value)))
  def get(value: Int) = {
    if(!values.contains(value)) throw new IllegalArgumentException("Illegal value %s for Visibility".format(value))
    Visibility(value)
  }
}