package service

import cats.effect.IO
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.{HttpService, MediaType, Response, Uri}
import repository.PdvRepository


class PdvService(repository: PdvRepository) extends Http4sDsl[IO]{
  private implicit val encodeTypePdv: Encoder[TypePdv] = Encoder.encodeString.contramap[TypePdv](_.value)

  private implicit val decodeTypePdv: Decoder[TypePdv] = Decoder.decodeString.map[TypePdv](TypePdv.unsafeFromStringValue)


  private def result(result: Either[model.PdvNotFoundError.type, Pdv]): IO[Response[IO]] = {
    result match {
      case Left(PdvNotFoundError) => NotFound()
      case Right(pdv) => Ok(pdv.asJson)
    }
  }

  val service = HttpService[IO] {
    case GET -> Root / "pdvs" =>
      Ok(Stream("[") ++ repository.getPdvs.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))

    case GET -> Root / "pdvs" / LongVar(id) =>
      for {
        getResult <- repository.getPdv(id)
        response <- result(getResult)
      } yield response

    case req @ POST -> Root / "pdvs" =>
      for {
        pdv <- req.decodeJson[Pdv]
        createdPdv <- repository.createPdv(pdv)
        response <- Created(createdPdv.asJson, Location(Uri.unsafeFromString(s"/pdvs/${createdPdv.id.get}")))
      } yield response

    case DELETE -> Root / "pdvs" / LongVar(id) =>
      repository.deletePdv(id).flatMap {
        case Left(PdvNotFoundError) => NotFound()
        case Right(_) => NoContent()
      }
  }

}
