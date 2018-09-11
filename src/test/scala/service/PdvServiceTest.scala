package service

import cats.effect.IO
import fs2.Stream
import io.circe.Json
import io.circe.literal._
import model._
import org.http4s.circe._
import org.http4s.dsl.io.{POST, uri, _}
import org.http4s.{HttpService, Request, Response, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import repository.PdvRepository


class PdvServiceTest extends WordSpec with MockFactory with Matchers {

  private val repository: PdvRepository = stub[PdvRepository]

  private val service: HttpService[IO] = new PdvService(repository).service

  "PdvService" should {
    "create a pdv" in {
      val id = 1
      val pdv = Pdv(None, "my first pdv", Drive)
      (repository.createPdv _).when(pdv).returns(IO.pure(pdv.copy(id = Some(id))))
      val createJson = json"""
        {
          "description": ${pdv.description},
          "typePdv": ${pdv.typePdv.value}
        }"""

      val response = serve(Request[IO](POST, uri("/pdvs")).withBody(createJson).unsafeRunSync())
      response.status shouldBe Status.Created
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": ${pdv.description},
          "typePdv": ${pdv.typePdv.value}
        }"""
    }

    "return a single pdv" in {
      val id = 1
      val pdv = Pdv(Some(id), "my pdv", Carrelage)
      (repository.getPdv _).when(id).returns(IO.pure(Right(pdv)))

      val response = serve(Request[IO](GET, Uri.unsafeFromString(s"/pdvs/$id")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": ${pdv.description},
          "typePdv": ${pdv.typePdv.value}
        }"""
    }


    "return all pdvs" in {
      val id1 = 1
      val pdv1 = Pdv(Some(id1), "my pdv 1", Carrelage)
      val id2 = 2
      val pdv2 = Pdv(Some(id2), "my pdv 2", Satellite)
      val pdvs = Stream(pdv1, pdv2)
      (repository.getPdvs _).when().returns(pdvs)

      val response = serve(Request[IO](GET, uri("/pdvs")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        [
         {
           "id": $id1,
           "description": ${pdv1.description},
           "typePdv": ${pdv1.typePdv.value}
         },
         {
           "id": $id2,
           "description": ${pdv2.description},
           "typePdv": ${pdv2.typePdv.value}
         }
        ]"""
    }

    "delete a pdv" in {
      val id = 1
      (repository.deletePdv _).when(id).returns(IO.pure(Right(())))

      val response = serve(Request[IO](DELETE, Uri.unsafeFromString(s"/pdvs/$id")))
      response.status shouldBe Status.NoContent
    }

  }


  private def serve(request: Request[IO]): Response[IO] = {
    service.orNotFound(request).unsafeRunSync()
  }

}
