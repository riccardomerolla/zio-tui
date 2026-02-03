package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.test.*
import zio.test.Assertion.*

object TestHelpersSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("TestHelpers")(
    suite("testWith")(
      test("provides layer to program and returns result") {
        val testData = Chunk(
          DataPoint(1000, 42.0, "temperature"),
          DataPoint(2000, 75.0, "humidity")
        )
        val layer   = DataSource.test(testData*)
        val program = DataSource.get("temperature")

        TestHelpers.testWith(layer)(program).map { point =>
          assertTrue(
            point.timestamp == 1000,
            point.value == 42.0,
            point.label == "temperature"
          )
        }
      },
      test("works with programs that succeed") {
        val testData = Chunk(DataPoint(1000, 100.0, "metric"))
        val layer   = DataSource.test(testData*)
        val program = ZIO.succeed(42)

        TestHelpers.testWith(layer)(program).map { result =>
          assertTrue(result == 42)
        }
      },
      test("propagates errors from program") {
        val layer   = DataSource.test()
        val program = DataSource.get("nonexistent")

        TestHelpers.testWith(layer)(program).exit.map { result =>
          assertTrue(
            result == Exit.fail(DataSourceError.NotFound("nonexistent"))
          )
        }
      },
      test("works with effects that require the service") {
        val testData = Chunk(
          DataPoint(1000, 10.0, "a"),
          DataPoint(2000, 20.0, "b")
        )
        val layer = DataSource.test(testData*)
        val program = for
          a <- DataSource.get("a")
          b <- DataSource.get("b")
        yield a.value + b.value

        TestHelpers.testWith(layer)(program).map { sum =>
          assertTrue(sum == 30.0)
        }
      }
    ),
    suite("failingTest")(
      test("succeeds when program fails with expected error") {
        val layer   = DataSource.test()
        val program = DataSource.get("missing")
        val assertion = (e: DataSourceError) => e match
          case DataSourceError.NotFound(id) => id == "missing"
          case _                            => false

        TestHelpers.failingTest(layer)(program)(assertion)
      },
      test("fails when program succeeds") {
        val testData = Chunk(DataPoint(1000, 42.0, "exists"))
        val layer   = DataSource.test(testData*)
        val program = DataSource.get("exists")
        val assertion = (e: DataSourceError) => true

        TestHelpers.failingTest(layer)(program)(assertion).map { result =>
          assertTrue(!result.isSuccess)
        }
      },
      test("fails when error doesn't match assertion") {
        val layer   = DataSource.test()
        val program = DataSource.get("wrong")
        val assertion = (e: DataSourceError) => e match
          case DataSourceError.NotFound(id) => id == "expected"
          case _                            => false

        TestHelpers.failingTest(layer)(program)(assertion).map { result =>
          assertTrue(!result.isSuccess)
        }
      },
      test("succeeds when error matches complex assertion") {
        val layer   = DataSource.test()
        val program = DataSource.get("item123")
        val assertion = (e: DataSourceError) => e match
          case DataSourceError.NotFound(id) => id.startsWith("item")
          case _                            => false

        TestHelpers.failingTest(layer)(program)(assertion)
      },
      test("handles different error types") {
        // Create a program that fails with a different error type
        val layer = DataSource.test()
        val program: ZIO[DataSource, DataSourceError, DataPoint] =
          ZIO.fail(DataSourceError.InvalidData("bad format"))
        val assertion = (e: DataSourceError) => e match
          case DataSourceError.InvalidData(msg) => msg == "bad format"
          case _                                => false

        TestHelpers.failingTest(layer)(program)(assertion)
      }
    )
  )
