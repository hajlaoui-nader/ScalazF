package repository

import cats.effect.IO
import doobie.Meta
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import model.{Pdv, TypePdv, PdvNotFoundError}

class PdvRepository(transactor: Transactor[IO]) {

  private implicit val typePdvMeta: Meta[TypePdv] = Meta[String].xmap(TypePdv.unsafeFromStringValue, _.value)

  def getPdvs: Stream[IO, Pdv] = {
    sql"SELECT id, description, typePdv FROM pdv".query[Pdv].stream.transact(transactor)
  }

  def getPdv(id: Long): IO[Either[PdvNotFoundError.type , Pdv]] = {
    sql"SELECT id, description, typePdv FROM pdv WHERE id = $id".query[Pdv].option.transact(transactor).map {
      case Some(pdv) => Right(pdv)
      case None => Left(PdvNotFoundError)
    }
  }

  def createPdv(pdv: Pdv): IO[Pdv] = {
    sql"INSERT INTO pdv (description, TypePdv) VALUES (${pdv.description}, ${pdv.typePdv})".update.withUniqueGeneratedKeys[Long]("id").transact(transactor).map { id =>
      pdv.copy(id = Some(id))
    }
  }

}
