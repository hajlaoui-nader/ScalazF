A sample implementation of a microservice using [http4s](http://http4s.org/), [doobie](http://tpolecat.github.io/doobie/),
[circe](https://github.com/circe/circe), pure config and a h2 in memory database.

This project uses [cats-effect](https://github.com/typelevel/cats-effect), By using an effect monad, side effects are postponed until the end of the world.
 

## End points
The end points are:

Method | Url         | Description
------ | ----------- | -----------
GET    | /pdvs       | Returns all pdvs.
GET    | /pdvs/{id}  | Returns the pdv for the specified id, 404 when no pdv present with this id.
POST   | /pdvs       | Creates a pdv, give as body JSON with the fields, returns a 201 with the created pdv.
DELETE | /pdvs/{id}  | Deletes the pdv with the specified id, 404 when no todo present with this id.
