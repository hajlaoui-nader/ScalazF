package object model {
  abstract sealed class TypePdv(val value: String)
  case object Drive extends TypePdv("drive")
  case object Satellite extends TypePdv("satellite")
  case object Carrelage extends TypePdv("carrelage")

  object TypePdv {
    private def values = Set(Drive, Satellite, Carrelage)

    def unsafeFromStringValue(value: String): TypePdv = {
      // TODO how to improve this .get
      values.find(_.value == value).get
    }
  }

  case class Pdv(id: Option[Long], description: String, typePdv: TypePdv)

  case object PdvNotFoundError

}
