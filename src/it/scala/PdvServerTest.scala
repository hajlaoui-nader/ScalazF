import cats.effect.IO
import config.Config
import db.Database
import io.circe.Json
import io.circe.literal._
import org.http4s.circe._
import org.http4s.client.blaze.Http1Client
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import io.circe.optics.JsonPath._
import org.http4s.server.{Server => Http4sServer}
import org.http4s.server.blaze.BlazeBuilder
import repository.PdvRepository
import service.PdvService

class PdvServerTest extends WordSpec with Matchers with BeforeAndAfterAll {
  private lazy val client = Http1Client[IO]().unsafeRunSync()

  private lazy val config = Config.load("test.conf").unsafeRunSync()

  private lazy val urlStart = s"http://${config.server.host}:${config.server.port}"

  private val server = createServer().unsafeRunSync()

  override def afterAll(): Unit = {
    client.shutdown.unsafeRunSync()
    server.shutdown.unsafeRunSync()
  }


  "Pdv Server" should {
    "create a pdv" in {
      val description = "my pdv 1"
      val typePdv = "carrelage"
      val createJson =json"""
        {
          "description": $description,
          "typePdv": $typePdv
        }"""
      val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"$urlStart/pdvs")).withBody(createJson).unsafeRunSync()
      val json = client.expect[Json](request).unsafeRunSync()
      root.id.long.getOption(json).nonEmpty shouldBe true
      root.description.string.getOption(json) shouldBe Some(description)
      root.typePdv.string.getOption(json) shouldBe Some(typePdv)
    }

    "return a single pdv" in {
      val description = "my pdv 2"
      val typePdv = "satellite"
      val id = createPdv(description, typePdv)
      client.expect[Json](Uri.unsafeFromString(s"$urlStart/pdvs/$id")).unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": $description,
          "typePdv": $typePdv
        }"""
    }

    "delete a pdv" in {
      val description = "my pdv 3"
      val typePdv = "carrelage"
      val id = createPdv(description, typePdv)
      val deleteRequest = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(s"$urlStart/pdvs/$id"))
      client.status(deleteRequest).unsafeRunSync() shouldBe Status.NoContent

      val getRequest = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/pdvs/$id"))
      client.status(getRequest).unsafeRunSync() shouldBe Status.NotFound
    }

    "return all pdvs" in {
      // Remove all existing pdvs
      val json = client.expect[Json](Uri.unsafeFromString(s"$urlStart/pdvs")).unsafeRunSync()
      root.each.id.long.getAll(json).foreach { id =>
        val deleteRequest = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(s"$urlStart/pdvs/$id"))
        client.status(deleteRequest).unsafeRunSync() shouldBe Status.NoContent
      }

      // Add new pdvs
      val description1 = "my pdv 1"
      val description2 = "my pdv 2"
      val typePdv1 = "carrelage"
      val typePdv2 = "drive"
      val id1 = createPdv(description1, typePdv1)
      val id2 = createPdv(description2, typePdv2)

      // Retrieve pdvs
      client.expect[Json](Uri.unsafeFromString(s"$urlStart/pdvs")).unsafeRunSync shouldBe json"""
        [
          {
            "id": $id1,
            "description": $description1,
            "typePdv": $typePdv1
          },
          {
            "id": $id2,
            "description": $description2,
            "typePdv": $typePdv2
          }
        ]"""
    }
  }

  private def createPdv(description: String, typePdv: String): Long = {
    val createJson =json"""
      {
        "description": $description,
        "typePdv": $typePdv
      }"""
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"$urlStart/pdvs")).withBody(createJson).unsafeRunSync()
    val json = client.expect[Json](request).unsafeRunSync()
    root.id.long.getOption(json).nonEmpty shouldBe true
    root.id.long.getOption(json).get
  }

  private def createServer(): IO[Http4sServer[IO]] = {
    for {
      transactor <- Database.transactor(config.database)
      _ <- Database.initialize(transactor)
      repository = new PdvRepository(transactor)
      server <- BlazeBuilder[IO]
        .bindHttp(config.server.port, config.server.host)
        .mountService(new PdvService(repository).service, "/").start
    } yield server
  }
}
