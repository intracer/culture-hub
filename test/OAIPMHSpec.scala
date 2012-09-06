import core.harvesting.OaiPmhService
import java.util.Date
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._

/**
 * TODO actually test the things we get back
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

class OAIPMHSpec extends Specs2TestContext {

  step {
    loadStandalone()
  }

  val spec = "sample-a"

  "the OAI-PMH repository" should {

    "return a valid identify response" in {

      withTestConfig {

        val request = FakeRequest("GET", "?verb=Identify")
        val r = controllers.api.OaiPmh.oaipmh("delving", None, None)(request)

        val response = asyncToResult(r)

        status(response) must equalTo(OK)

        val xml = contentAsXML(response)

        val error = xml \ "error"
        error.length must equalTo(0)
      }

    }

    "list sets" in {

      withTestConfig {

        val request = FakeRequest("GET", "?verb=ListSets")
        val r = controllers.api.OaiPmh.oaipmh("delving", None, None)(request)

        val response = asyncToResult(r)

        status(response) must equalTo(OK)


        val xml = contentAsXML(response)
        val error = xml \ "error"
        if (error.length != 0) println(error)
        error.length must equalTo(0)

        val sets = xml \ "ListSets" \ "set"
        sets.size must equalTo(2)
        (sets \ "setSpec").map(_.text).toSeq must equalTo(Seq("sample-a", "sample-b"))
      }
    }

    "list formats" in {

      withTestConfig {
        val request = FakeRequest("GET", "?verb=ListMetadataFormats")
        val r = controllers.api.OaiPmh.oaipmh("delving", Some("icn"), None)(request)

        val response = asyncToResult(r)

        status(response) must equalTo(OK)

        (contentAsXML(response) \\ "metadataFormat").size must equalTo (1)
        ((contentAsXML(response) \\ "metadataFormat").head \ "metadataPrefix").text must equalTo("icn")

      }

    }

    "list records" in {

      withTestConfig {

        val request = FakeRequest("GET", "?verb=ListRecords&set="+spec+"&metadataPrefix=icn")
        val r = controllers.api.OaiPmh.oaipmh("delving", None, None)(request)

        val response = asyncToResult(r)

        status(response) must equalTo(OK)

        val xml = contentAsXML(response)
        val error = xml \ "error"
        error.length must equalTo(0)

        val records = xml \ "ListRecords" \ "record"
        records.length must equalTo(7) // 7 valid records for ICN
      }

    }

    "list records with a 'from' datestamp using YYYYMMDD format" in {
      withTestConfig {
        val today = OaiPmhService.dateFormat.format(new Date())
        val request = FakeRequest("GET", "?verb=ListRecords&set="+spec+"&metadataPrefix=icn&from=" + today)
        val r = controllers.api.OaiPmh.oaipmh("delving", None, None)(request)

        val response = asyncToResult(r)

        status(response) must equalTo(OK)

        val xml = contentAsXML(response)
        val error = xml \ "error"
        error.length must equalTo(0)

        val records = xml \ "ListRecords" \ "record"
        records.length must equalTo(7) // 7 valid records
      }
    }


    "list records with an 'until' datestamp using YYYYMMDD format" in {
      withTestConfig {
        val today = OaiPmhService.dateFormat.format(new Date())
        val request = FakeRequest("GET", "?verb=ListRecords&set="+spec+"&metadataPrefix=icn&until=" + today)
        val r = controllers.api.OaiPmh.oaipmh("delving", None, None)(request)

        val response = asyncToResult(r)

        status(response) must equalTo(OK)

        val xml = contentAsXML(response)
        val error = xml \ "error"
        error.length must equalTo(1)
        (error \ "@code").text must equalTo("noRecordsMatch")
      }
    }


    "list no records with a 'from' datestamp using a future YYYYMMDD format" in {
      withTestConfig {
        val d = new DateTime()
        val tomorrow = OaiPmhService.dateFormat.format(d.plusDays(1).toDate)
        val request = FakeRequest("GET", "?verb=ListRecords&set="+spec+"&metadataPrefix=icn&from=" + tomorrow)
        val r = controllers.api.OaiPmh.oaipmh("delving", None, None)(request)

        val response = asyncToResult(r)

        status(response) must equalTo(OK)

        val xml = contentAsXML(response)
        val error = xml \ "error"
        error.length must equalTo(1)
        (error \ "@code").text must equalTo("noRecordsMatch")
      }
    }
  }

  "list no records with an 'until' datestamp using a past YYYYMMDD format" in {
    withTestConfig {
      val d = new DateTime()
      val yesterday = OaiPmhService.dateFormat.format(d.minusDays(1).toDate)
      val request = FakeRequest("GET", "?verb=ListRecords&set="+spec+"&metadataPrefix=icn&until=" + yesterday)
      val r = controllers.api.OaiPmh.oaipmh("delving", None, None)(request)

      val response = asyncToResult(r)

      status(response) must equalTo(OK)

      val xml = contentAsXML(response)
      val error = xml \ "error"
      error.length must equalTo(1)
      (error \ "@code").text must equalTo("noRecordsMatch")
    }
  }


  step(cleanup())


}
