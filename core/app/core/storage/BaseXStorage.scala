package core.storage

import eu.delving.basex.client._
import eu.delving.basex.client.BaseX
import org.basex.server.ClientSession
import java.io.ByteArrayInputStream
import scala.xml.Node
import scala._

/**
 * BaseX-based Storage engine.
 *
 * One BaseX db == One collection
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object BaseXStorage {

  // TODO use real config, and non-embedded
  val storage = new BaseX("localhost", 1984, "admin", "admin")

  def createCollection(orgId: String, collectionName: String): Collection = {
    val c = Collection(orgId, collectionName)
    storage.createDatabase(c.databaseName)
    c
  }

  def openCollection(orgId: String, collectionName: String): Option[Collection] = {
    val c = Collection(orgId, collectionName)
    try {
      storage.openDatabase(c.databaseName)
      Some(c)
    } catch {
      case _ => None
    }
  }

  def withSession[T](collection: Collection)(block: ClientSession => T) = {
    storage.withSession(collection.databaseName) {
      session =>
        block(session)
    }
  }

  def withBulkSession[T](collection: Collection)(block: ClientSession => T) = {
    storage.withSession(collection.databaseName) {
      session =>
        session.setAutoflush(false)
        block(session)
        session.setAutoflush(true)
    }
  }

  def store(collection: Collection, records: Iterator[Record], namespaces: Map[String, String]): Int = {
    var inserted: Int = 0
    BaseXStorage.withBulkSession(collection) {
      session =>
        val versions: Map[String, Int] = (session.find("""for $i in /record let $id := $i/@id group by $id return <version id="{$id}">{count($i)}</version>""") map {
          v: Node =>
            ((v \ "@id").text -> v.text.toInt)
        }).toMap

        val it = records.zipWithIndex
        while(it.hasNext) {
          val next = it.next()
          if(next._2 % 10000 == 0) session.flush()
          session.add(next._1.id, buildRecord(next._1, versions.get(next._1.id).getOrElse(0), namespaces, next._2))
          inserted = next._2
        }
        session.flush()
        session.createAttributeIndex()
      }
    inserted
  }

  def buildRecord(record: Record, version: Int, namespaces: Map[String, String], index: Int) = {

    val ns = namespaces.map(ns => """xmlns:%s="%s"""".format(ns._1, ns._2)).mkString(" ")

    new ByteArrayInputStream("""<record id="%s" %s>
      <system>
        <version>%s</version>
        <schemaPrefix>%s</schemaPrefix>
        <index>%s</index>
        <invalidTargetSchemas>%s</invalidTargetSchemas>
      </system>
      <document>%s</document>
      <links/>
    </record>""".format(record.id, ns, version, record.schemaPrefix, index, record.invalidTargetSchemas.mkString(",").mkString, record.document).getBytes("utf-8"))

  }

}

case class Record(id: String, schemaPrefix: String, document: String, invalidTargetSchemas: List[String] = List.empty)

case class Collection(orgId: String, name: String) {
  val databaseName = orgId + "____" + name
}



