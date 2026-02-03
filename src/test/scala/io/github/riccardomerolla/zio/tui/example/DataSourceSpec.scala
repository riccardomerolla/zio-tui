package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.test.*
import zio.test.Assertion.*

object DataSourceSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("DataSource")(
    suite("Live implementation")(
      suite("get")(
        test("retrieves saved data point by label") {
          val point = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
          val program = for
            _      <- DataSource.save(point)
            result <- DataSource.get("test")
          yield assertTrue(
            result.timestamp == 1000L,
            result.value == 42.0,
            result.label == "test",
          )

          program.provide(DataSource.live)
        },
        test("fails with NotFound for non-existent label") {
          val program = for
            result <- DataSource.get("nonexistent").exit
          yield assertTrue(
            result == Exit.fail(DataSourceError.NotFound("nonexistent"))
          )

          program.provide(DataSource.live)
        },
        test("retrieves updated data point after save") {
          val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "metric")
          val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "metric")
          val program = for
            _      <- DataSource.save(point1)
            _      <- DataSource.save(point2)
            result <- DataSource.get("metric")
          yield assertTrue(
            result.timestamp == 2000L,
            result.value == 84.0,
          )

          program.provide(DataSource.live)
        },
      ),
      suite("save")(
        test("stores data point successfully") {
          val point = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
          val program = for
            _      <- DataSource.save(point)
            result <- DataSource.get("test")
          yield assertTrue(result == point)

          program.provide(DataSource.live)
        },
        test("overwrites existing data point with same label") {
          val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "metric")
          val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "metric")
          val program = for
            _      <- DataSource.save(point1)
            _      <- DataSource.save(point2)
            result <- DataSource.get("metric")
          yield assertTrue(result == point2)

          program.provide(DataSource.live)
        },
        test("stores multiple distinct data points") {
          val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "cpu")
          val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "memory")
          val program = for
            _       <- DataSource.save(point1)
            _       <- DataSource.save(point2)
            result1 <- DataSource.get("cpu")
            result2 <- DataSource.get("memory")
          yield assertTrue(
            result1 == point1,
            result2 == point2,
          )

          program.provide(DataSource.live)
        },
      ),
      suite("stream")(
        test("streams all stored data points") {
          val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "cpu")
          val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "memory")
          val point3 = DataPoint(timestamp = 3000L, value = 100.0, label = "disk")
          val program = for
            _       <- DataSource.save(point1)
            _       <- DataSource.save(point2)
            _       <- DataSource.save(point3)
            service <- ZIO.service[DataSource]
            results <- service.stream.runCollect
          yield assertTrue(
            results.size == 3,
            results.toSet == Set(point1, point2, point3),
          )

          program.provide(DataSource.live)
        },
        test("streams empty when no data points stored") {
          val program = for
            service <- ZIO.service[DataSource]
            results <- service.stream.runCollect
          yield assertTrue(results.isEmpty)

          program.provide(DataSource.live)
        },
        test("streams updated data after save operations") {
          val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "metric")
          val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "metric")
          val program = for
            _        <- DataSource.save(point1)
            service1 <- ZIO.service[DataSource]
            results1 <- service1.stream.runCollect
            _        <- DataSource.save(point2)
            service2 <- ZIO.service[DataSource]
            results2 <- service2.stream.runCollect
          yield assertTrue(
            results1.size == 1,
            results1.head.value == 42.0,
            results2.size == 1,
            results2.head.value == 84.0,
          )

          program.provide(DataSource.live)
        },
      ),
    ),
    suite("Test implementation")(
      suite("get")(
        test("retrieves data point by label") {
          val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "cpu")
          val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "memory")
          val testLayer = DataSource.test(point1, point2)

          for result <- DataSource.get("cpu").provide(testLayer)
          yield assertTrue(result == point1)
        },
        test("fails with NotFound for non-existent label") {
          val point = DataPoint(timestamp = 1000L, value = 42.0, label = "cpu")
          val testLayer = DataSource.test(point)

          for result <- DataSource.get("memory").provide(testLayer).exit
          yield assertTrue(
            result == Exit.fail(DataSourceError.NotFound("memory"))
          )
        },
        test("works with empty test data") {
          val testLayer = DataSource.test()

          for result <- DataSource.get("anything").provide(testLayer).exit
          yield assertTrue(
            result == Exit.fail(DataSourceError.NotFound("anything"))
          )
        },
      ),
      suite("save")(
        test("succeeds without storing data") {
          val point = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
          val testLayer = DataSource.test()

          for
            _      <- DataSource.save(point).provide(testLayer)
            result <- DataSource.get("test").provide(testLayer).exit
          yield assertTrue(
            result == Exit.fail(DataSourceError.NotFound("test"))
          )
        },
      ),
      suite("stream")(
        test("streams provided test data") {
          val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "cpu")
          val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "memory")
          val point3 = DataPoint(timestamp = 3000L, value = 100.0, label = "disk")
          val testLayer = DataSource.test(point1, point2, point3)

          for
            service <- ZIO.service[DataSource].provide(testLayer)
            results <- service.stream.runCollect
          yield assertTrue(
            results.size == 3,
            results == Chunk(point1, point2, point3),
          )
        },
        test("streams empty for empty test data") {
          val testLayer = DataSource.test()

          for
            service <- ZIO.service[DataSource].provide(testLayer)
            results <- service.stream.runCollect
          yield assertTrue(results.isEmpty)
        },
        test("preserves order of test data") {
          val point1 = DataPoint(timestamp = 3000L, value = 100.0, label = "third")
          val point2 = DataPoint(timestamp = 1000L, value = 42.0, label = "first")
          val point3 = DataPoint(timestamp = 2000L, value = 84.0, label = "second")
          val testLayer = DataSource.test(point1, point2, point3)

          for
            service <- ZIO.service[DataSource].provide(testLayer)
            results <- service.stream.runCollect
          yield assertTrue(results == Chunk(point1, point2, point3))
        },
      ),
    ),
    suite("accessor methods")(
      test("DataSource.get accesses service from environment") {
        val point = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
        val testLayer = DataSource.test(point)

        for result <- DataSource.get("test").provide(testLayer)
        yield assertTrue(result == point)
      },
      test("DataSource.save accesses service from environment") {
        val point = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
        val program = for
          _      <- DataSource.save(point)
          result <- DataSource.get("test")
        yield assertTrue(result == point)

        program.provide(DataSource.live)
      },
      test("DataSource.stream accesses service from environment") {
        val point = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
        val testLayer = DataSource.test(point)

        for
          service <- ZIO.service[DataSource].provide(testLayer)
          results <- service.stream.runCollect
        yield assertTrue(results.size == 1)
      },
    ),
    suite("DataSourceError")(
      test("ConnectionFailed error stores reason") {
        val error = DataSourceError.ConnectionFailed("Network timeout")
        assertTrue(error match {
          case DataSourceError.ConnectionFailed(reason) => reason == "Network timeout"
          case _ => false
        })
      },
      test("InvalidData error stores message") {
        val error = DataSourceError.InvalidData("Malformed JSON")
        assertTrue(error match {
          case DataSourceError.InvalidData(message) => message == "Malformed JSON"
          case _ => false
        })
      },
      test("NotFound error stores id") {
        val error = DataSourceError.NotFound("missing-key")
        assertTrue(error match {
          case DataSourceError.NotFound(id) => id == "missing-key"
          case _ => false
        })
      },
      test("errors extend Exception") {
        val error: Exception = DataSourceError.NotFound("test")
        assertTrue(error.isInstanceOf[Exception])
      },
    ),
    suite("DataPoint")(
      test("can be created with case class constructor") {
        val point = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
        assertTrue(
          point.timestamp == 1000L,
          point.value == 42.0,
          point.label == "test",
        )
      },
      test("supports equality comparison") {
        val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
        val point2 = DataPoint(timestamp = 1000L, value = 42.0, label = "test")
        val point3 = DataPoint(timestamp = 2000L, value = 42.0, label = "test")
        assertTrue(
          point1 == point2,
          point1 != point3,
        )
      },
    ),
    suite("Layer composition")(
      test("live layer can be composed with other layers") {
        val program = for
          point  <- ZIO.succeed(DataPoint(timestamp = 1000L, value = 42.0, label = "test"))
          _      <- DataSource.save(point)
          result <- DataSource.get("test")
        yield assertTrue(
          result.timestamp == 1000L,
          result.value == 42.0,
        )

        program.provide(DataSource.live)
      },
      test("test layer allows deterministic testing") {
        val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "cpu")
        val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "memory")

        val program = for
          cpu    <- DataSource.get("cpu")
          memory <- DataSource.get("memory")
        yield (cpu.value, memory.value)

        for result <- program.provide(DataSource.test(point1, point2))
        yield assertTrue(result == (42.0, 84.0))
      },
    ),
  )
