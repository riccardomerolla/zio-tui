package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.stream.*

// Domain model
case class DataPoint(timestamp: Long, value: Double, label: String)

// Typed errors
enum DataSourceError extends Exception:
  case ConnectionFailed(reason: String) extends DataSourceError
  case InvalidData(message: String) extends DataSourceError
  case NotFound(id: String) extends DataSourceError

// Service trait
trait DataSource:
  def stream: ZStream[Any, DataSourceError, DataPoint]
  def get(id: String): IO[DataSourceError, DataPoint]
  def save(point: DataPoint): IO[DataSourceError, Unit]

// Companion object with layers and accessors
object DataSource:
  // Accessor methods
  def stream: ZStream[DataSource, DataSourceError, DataPoint] =
    ZStream.serviceWithStream(_.stream)

  def get(id: String): ZIO[DataSource, DataSourceError, DataPoint] =
    ZIO.serviceWithZIO(_.get(id))

  def save(point: DataPoint): ZIO[DataSource, DataSourceError, Unit] =
    ZIO.serviceWithZIO(_.save(point))

  // Live implementation
  case class Live(storage: Ref[Map[String, DataPoint]]) extends DataSource:
    def stream: ZStream[Any, DataSourceError, DataPoint] =
      ZStream.fromZIO(storage.get).flatMap(map => ZStream.fromIterable(map.values))

    def get(id: String): IO[DataSourceError, DataPoint] =
      storage.get.flatMap { map =>
        ZIO.fromOption(map.get(id))
          .orElseFail(DataSourceError.NotFound(id))
      }

    def save(point: DataPoint): IO[DataSourceError, Unit] =
      storage.update(_ + (point.label -> point))

  // Test implementation
  case class Test(data: Chunk[DataPoint]) extends DataSource:
    def stream: ZStream[Any, DataSourceError, DataPoint] =
      ZStream.fromIterable(data)

    def get(id: String): IO[DataSourceError, DataPoint] =
      ZIO.fromOption(data.find(_.label == id))
        .orElseFail(DataSourceError.NotFound(id))

    def save(point: DataPoint): IO[DataSourceError, Unit] =
      ZIO.unit

  // Layer constructors
  val live: ZLayer[Any, Nothing, DataSource] =
    ZLayer.fromZIO(
      Ref.make(Map.empty[String, DataPoint]).map(Live(_))
    )

  def test(data: DataPoint*): ZLayer[Any, Nothing, DataSource] =
    ZLayer.succeed(Test(Chunk.fromIterable(data)))
