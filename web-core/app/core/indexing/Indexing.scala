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

package core.indexing

import extensions.HTTPClient
import org.apache.solr.common.SolrInputDocument
import play.api.Logger
import core.Constants._
import org.apache.commons.httpclient.methods.GetMethod
import java.io.{InputStream, FilenameFilter, File}
import org.apache.tika.sax.BodyContentHandler
import org.apache.tika.parser.pdf.PDFParser
import exceptions.SolrConnectionException
import core.search.{SolrBindingService, SolrServer}
import org.apache.tika.parser.ParseContext
import org.apache.tika.metadata.Metadata
import java.net.URLEncoder
import models.{DomainConfiguration, Visibility, MetadataItem, DataSet}
import org.apache.commons.lang.{StringEscapeUtils, StringUtils}


/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Indexing extends SolrServer {

  def indexOne(dataSet: DataSet, mdr: MetadataItem, mapped: Map[String, List[Any]], metadataFormatForIndexing: String)(implicit configuration: DomainConfiguration): Either[Throwable, String] = {
    val doc = createSolrInputDocument(mapped)
    addDelvingHouseKeepingFields(doc, dataSet, mdr, metadataFormatForIndexing)
    try {
      IndexingService.stageForIndexing(doc)
    } catch {
      case t: Throwable => Left(new SolrConnectionException("Unable to add document to Solr", t))
    }
    Right("ok")
  }

  private def createSolrInputDocument(indexDoc: Map[String, List[Any]]): SolrInputDocument = {

    val doc = new SolrInputDocument
    indexDoc.foreach {
      entry =>
        val unMungedKey = entry._1
        entry._2.foreach {
          value =>
            val cleanValue = if (unMungedKey.endsWith("_text")) StringEscapeUtils.unescapeHtml(value.toString) else value.toString
            doc.addField(unMungedKey, cleanValue)
        }
    }
    doc
  }

  def addDelvingHouseKeepingFields(inputDoc: SolrInputDocument, dataSet: DataSet, record: MetadataItem, schemaPrefix: String) {
    import scala.collection.JavaConversions._

    // mandatory fields
    inputDoc.addField(ORG_ID, dataSet.orgId)
    inputDoc.addField(VISIBILITY, Visibility.PUBLIC.value.toString)
    inputDoc.addField(RECORD_TYPE, MDR)
    inputDoc.addField(SYSTEM_TYPE, HUB_ITEM)

    inputDoc.addField(HUB_ID, URLEncoder.encode(record.itemId, "utf-8"))
    inputDoc.addField(SPEC, "%s".format(dataSet.spec))
    inputDoc.addField(SCHEMA, schemaPrefix)

    // for backwards-compatibility
    inputDoc.addField(PMH_ID, URLEncoder.encode(record.itemId, "utf-8"))

    // force the provider and dataProvider configured in the DataSet
    if(inputDoc.containsKey(PROVIDER)) {
      inputDoc.remove(PROVIDER)
      inputDoc.addField(PROVIDER, dataSet.getProvider)
    }
    if(inputDoc.containsKey(OWNER)) {
      inputDoc.remove(OWNER)
      inputDoc.addField(OWNER, dataSet.getDataProvider)
    }

    // deepZoom hack
    val DEEPZOOMURL: String = "delving_deepZoomUrl_string"
    val DEEPZOOM_PATH: String = "/iip/deepzoom"
    if(inputDoc.containsKey(DEEPZOOMURL)) {
      try {
        // http://some.delving.org/iip/deepzoom/mnt/tib/tiles/<orgId>/<spec>/<image>
        val url = inputDoc.get(DEEPZOOMURL).getFirstValue.toString
        val i = url.indexOf(DEEPZOOM_PATH)
        if(i > -1) {
          val tileSetPath = url.substring(i + DEEPZOOM_PATH.length(), url.length())
          val tileSetParentPath = tileSetPath.substring(0, tileSetPath.lastIndexOf(File.separator))
          val parent = new File(tileSetParentPath)
          val extensionIdx = if(tileSetPath.indexOf(".") > -1) tileSetPath.lastIndexOf(".") else tileSetPath.length()
          val imageIdx = tileSetPath.lastIndexOf(File.separator)
          val image = tileSetPath.substring(imageIdx + 1, extensionIdx)
          if(!(parent.exists() && parent.isDirectory)) {
            Logger("CultureHub").debug("No tile path %s for deepZoomUrl %s".format(tileSetParentPath, url))
            inputDoc.remove(DEEPZOOMURL)
          } else {
            val files = parent.listFiles(new FilenameFilter() {
              def accept(dir: File, name: String) = name.startsWith(image)
            })
            if(files.length == 0) {
              Logger("CultureHub").debug("No image in directory %s starting with %s for deepZoomUrl %s".format(tileSetParentPath, image, url))
              inputDoc.remove(DEEPZOOMURL)
            }
          }
        }
      } catch {
        case t: Throwable =>
          Logger("CultureHub").error("Error during deepZoomUrl check, deepZoomURL " + inputDoc.get(DEEPZOOMURL).getFirstValue, t)
          // in doubt, remove
          inputDoc.remove(DEEPZOOMURL)
      }
    }

    // add full text from digital objects
    val fullTextUrl = "%s_string".format(FULL_TEXT_OBJECT_URL)
    if (inputDoc.containsKey(fullTextUrl)) {
      val pdfUrl = inputDoc.get(fullTextUrl).getFirstValue.toString
      if (pdfUrl.endsWith(".pdf")) {
        val fullText = TikaIndexer.getFullTextFromRemoteURL(pdfUrl)
        inputDoc.addField("delving_fullText_text", fullText)
      }
    }
    
    if (inputDoc.containsKey(ID)) inputDoc.remove(ID)
    inputDoc.addField(ID, record.itemId)

    val uriWithTypeSuffix = EUROPEANA_URI + "_string"
    val uriWithTextSuffix = EUROPEANA_URI + "_text"
    if (inputDoc.containsKey(uriWithTypeSuffix)) {
      val uriValue: String = inputDoc.get(uriWithTypeSuffix).getFirstValue.toString
      inputDoc.remove(uriWithTypeSuffix)
      inputDoc.addField(EUROPEANA_URI, uriValue)
    }
    else if (inputDoc.contains(uriWithTextSuffix)) {
      val uriValue: String = inputDoc.get(uriWithTextSuffix).getFirstValue.toString
      inputDoc.remove(uriWithTextSuffix)
      inputDoc.addField(EUROPEANA_URI, uriValue)
    }

    dataSet.getAllMappingSchemas.foreach(schema => inputDoc.addField(ALL_SCHEMAS, schema.prefix))

    val indexedKeys = inputDoc.keys.map(key => (SolrBindingService.stripDynamicFieldLabels(key), key)).toMap // to filter always index a facet with _facet .filter(!_.matches(".*_(s|string|link|single)$"))

    // add facets at indexing time
    dataSet.idxFacets.foreach {
      facet =>
        if (indexedKeys.contains(facet)) {
          val facetContent = inputDoc.get(indexedKeys.get(facet).get).getValues
          inputDoc addField("%s_facet".format(facet), facetContent)
          // enable case-insensitive autocomplete
          inputDoc addField ("%s_lowercase".format(facet), facetContent)
        }
    }
    // adding sort fields at index time
    dataSet.idxSortFields.foreach {
      sort =>
        if (indexedKeys.contains(sort)) {
          inputDoc addField("sort_all_%s".format(sort), inputDoc.get(indexedKeys.get(sort).get))
        }
    }
  }

}

object TikaIndexer extends HTTPClient {

  def getFullTextFromRemoteURL (url: String): Option[String] = {
    try {
      Some(parseFullTextFromPdf(getObject(url)))
    }
    catch {
      case e: Exception =>
        Logger.error("unable to process digital object found at " + url)
        None
    }
  }

  def getObject(url: String): InputStream  = {
    val method = new GetMethod(url)
    getHttpClient executeMethod (method)
    method.getResponseBodyAsStream
  }

  def parseFullTextFromPdf(input: InputStream): String = {
    val textHandler = new BodyContentHandler()
    val metadata = new Metadata()
    val parser = new PDFParser()
    parser.parse(input, textHandler, metadata, new ParseContext)
    input.close()
    textHandler.toString
  }
}