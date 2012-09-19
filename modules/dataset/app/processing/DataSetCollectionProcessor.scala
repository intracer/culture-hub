package processing

import play.api.Logger
import models._
import java.net.URL
import io.Source
import core.indexing.{IndexingService, Indexing}
import org.apache.solr.client.solrj.SolrQuery
import core.indexing.IndexField._
import core.SystemField._
import org.joda.time.DateTime
import core.HubServices
import core.processing.{CollectionProcessor, ProcessingSchema}


/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object DataSetCollectionProcessor {

  val log = Logger("CultureHub")

  val RAW_PREFIX = "raw"
  val AFF_PREFIX = "aff"

  def process(dataSet: DataSet)(implicit configuration: DomainConfiguration) {

    DataSet.dao.updateState(dataSet, DataSetState.PROCESSING)

    val invalidRecords = DataSet.dao.getInvalidRecords(dataSet)

    val selectedSchemas: Seq[RecordDefinition] = dataSet.mappings.flatMap(mapping => RecordDefinition.getRecordDefinition(mapping._2.schemaPrefix, mapping._2.schemaVersion)).toSeq

    val selectedProcessingSchemas: Seq[ProcessingSchema] = selectedSchemas map {
      t => new ProcessingSchema {
        val definition: RecordDefinition = t
        val namespaces: Map[String, String] = t.getNamespaces ++ dataSet.getNamespaces
        val mapping: Option[String] = if (dataSet.mappings.contains(t.prefix) && dataSet.mappings(t.prefix) != null) dataSet.mappings(t.prefix).recordMapping else None
        val sourceSchema: String = RAW_PREFIX

        def isValidRecord(index: Int): Boolean = definition.prefix == RAW_PREFIX || !invalidRecords(t.prefix).contains(index)
      }
    }

    val crosswalks: Seq[(RecordDefinition, URL)] = selectedSchemas.map(source => (source -> RecordDefinition.getCrosswalkResources(source.prefix))).flatMap(cw => cw._2.map(c => (cw._1, c)))
    val crosswalkSchemas: Seq[ProcessingSchema] = crosswalks flatMap {
      c =>
        val prefix = c._2.getPath.substring(c._2.getPath.indexOf(c._1.prefix + "-")).split("-")(0)
        val recordDefinition = RecordDefinition.getRecordDefinition(prefix, "1.0.0") // TODO versions for crosswalks

        if (recordDefinition.isDefined) {
          val schema = new ProcessingSchema {
            val definition = recordDefinition.get
            val namespaces: Map[String, String] = c._1.getNamespaces ++ dataSet.getNamespaces
            val mapping: Option[String] = Some(Source.fromURL(c._2).getLines().mkString("\n"))
            val sourceSchema: String = c._1.prefix

            def isValidRecord(index: Int): Boolean = true // TODO later we need to figure out a way to handle validation for crosswalks
          }
          Some(schema)
        } else {
          log.warn("Could not find RecordDefinition for schema '%s', which is the target of crosswalk '%s' - skipping it.".format(prefix, c._2))
          None
        }
    }

    val targetSchemas: List[ProcessingSchema] = selectedProcessingSchemas.toList ++ crosswalkSchemas.toList

    val isActionable: ProcessingSchema => Boolean = s => s.hasMapping || s.definition.prefix == RAW_PREFIX

    val actionableTargetSchemas = targetSchemas.partition(isActionable)._1
    val incompleteTargetSchemas = targetSchemas.partition(isActionable)._2

    if (!incompleteTargetSchemas.isEmpty) {
      log.warn("Could not find mapping for the following schemas: %s. They will be ignored in the mapping process.".format(incompleteTargetSchemas.mkString(", ")))
    }

    val indexingSchema: Option[ProcessingSchema] = dataSet.idxMappings.headOption.flatMap(i => actionableTargetSchemas.find(_.prefix == i))

    val renderingSchema: Option[ProcessingSchema] = if (actionableTargetSchemas.exists(_.prefix == AFF_PREFIX)) {
      actionableTargetSchemas.find(_.prefix == AFF_PREFIX)
    } else if (indexingSchema.isDefined) {
      indexingSchema
    } else {
      actionableTargetSchemas.headOption
    }

    val collectionProcessor = new CollectionProcessor(dataSet, actionableTargetSchemas, indexingSchema, renderingSchema, HubServices.basexStorage(configuration))
    def interrupted = {
      val current = DataSet.dao.getState(dataSet.orgId, dataSet.spec)
      current != DataSetState.PROCESSING && current != DataSetState.QUEUED
    }
    def updateCount(count: Long) {
      DataSet.dao.updateIndexingCount(dataSet, count)
    }
    def onError(t: Throwable) {
      DataSet.dao.updateState(dataSet, DataSetState.ERROR, None, Some(t.getMessage))
    }
    def indexOne(item: MetadataItem, fields: CollectionProcessor#MultiMap, prefix: String)(implicit configuration: DomainConfiguration) =
      Indexing.indexOne(dataSet, item, fields, prefix)

    def onIndexingComplete(start: DateTime) {

      // we retry this one 3 times, in order to minimize the chances of loosing the whole index if a timeout happens to occur
      var retries = 0
      var success = false
      while(retries < 3 && !success) {
        try {
          IndexingService.deleteOrphansBySpec(dataSet.orgId, dataSet.spec, start)
          success = true
        } catch {
          case t: Throwable => retries += 1
          }
        }
      if(!success) {
        log.error("Could not delete orphans records from SOLR. You may have to clean up by hand.")
      }

      // TODO move someplace else...
      // workaround: it looks as though the first query targeting a specific set after it has been indexed blows up with a timeout
      // it does not matter how long we wait
      // hence, we just trigger it here, and ignore the exception.
      try {
        IndexingService.runQuery(new SolrQuery("%s:%s %s:%s".format(SPEC.tag, dataSet.spec, ORG_ID.key, dataSet.orgId)))
      } catch {
        case t: Throwable => // as described earlier on, just ignore this exception
      }

    }

    collectionProcessor.process(interrupted, updateCount, onError, indexOne, onIndexingComplete)(configuration)
    val state = DataSet.dao.getState(dataSet.orgId, dataSet.spec)
    if(state == DataSetState.PROCESSING) {
      DataSet.dao.updateState(dataSet, DataSetState.ENABLED)
    } else if(state == DataSetState.CANCELLED) {
      DataSet.dao.updateState(dataSet, DataSetState.UPLOADED)
    }
  }


}
