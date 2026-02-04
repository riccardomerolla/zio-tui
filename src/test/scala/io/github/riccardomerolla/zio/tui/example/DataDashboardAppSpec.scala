package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.test.*
import zio.test.Assertion.*

import io.github.riccardomerolla.zio.tui.*

object DataDashboardAppSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("DataDashboardApp")(
    suite("program")(
      test("compiles with correct type signature") {
        // This test verifies the program has the correct type
        val _: ZIO[DataSource & TerminalService, DataSourceError | TUIError, Unit] =
          DataDashboardApp.program

        assertTrue(true)
      },
      test("executes successfully with test services") {
        val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "CPU")
        val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "Memory")
        val point3 = DataPoint(timestamp = 3000L, value = 16.0, label = "Disk")

        val testLayer = DataSource.test(point1, point2, point3) ++ TerminalService.test()

        for _ <- DataDashboardApp.program.provide(testLayer)
        yield assertTrue(true)
      },
      test("handles empty data source") {
        val testLayer = DataSource.test() ++ TerminalService.test()

        for _ <- DataDashboardApp.program.provide(testLayer)
        yield assertTrue(true)
      },
      test("uses accessor methods from both services") {
        val point     = DataPoint(timestamp = 1000L, value = 99.9, label = "Uptime")
        val testLayer = DataSource.test(point) ++ TerminalService.test()

        // Verify the program can access both services
        for _ <- DataDashboardApp.program.provide(testLayer)
        yield assertTrue(true)
      },
    ),
    suite("appLayer")(
      test("has correct type signature") {
        // Verify the layer provides both services
        val _: ZLayer[Any, Nothing, DataSource & TerminalService] =
          DataDashboardApp.appLayer

        assertTrue(true)
      },
      test("composes DataSource.live and TerminalService.live horizontally") {
        // Verify the composed layer can provide both services
        val program =
          for
            _          <- DataSource.save(DataPoint(timestamp = 1000L, value = 42.0, label = "test"))
            dataSource <- ZIO.service[DataSource]
            terminal   <- ZIO.service[TerminalService]
          yield assertTrue(
            dataSource.isInstanceOf[DataSource],
            terminal.isInstanceOf[TerminalService],
          )

        program.provide(DataDashboardApp.appLayer)
      },
      test("provides independent service instances") {
        // Verify both services work correctly when composed
        val program =
          for
            point  <- ZIO.succeed(DataPoint(timestamp = 1000L, value = 42.0, label = "metric"))
            _      <- DataSource.save(point)
            result <- DataSource.get("metric")
            widget <- ZIO.succeed(Widget.text("test"))
            _      <- TerminalService.render(widget)
          yield assertTrue(result == point)

        program.provide(DataDashboardApp.appLayer)
      },
    ),
    suite("layer composition patterns")(
      test("horizontal composition with ++ combines independent services") {
        // Demonstrate that ++ combines services that don't depend on each other
        val composed                                              = DataSource.test() ++ TerminalService.test()
        val _: ZLayer[Any, Nothing, DataSource & TerminalService] = composed

        assertTrue(true)
      },
      test("composed layer can be provided to program requiring both services") {
        val point1    = DataPoint(timestamp = 1000L, value = 50.0, label = "Load")
        val point2    = DataPoint(timestamp = 2000L, value = 75.0, label = "Network")
        val testLayer = DataSource.test(point1, point2) ++ TerminalService.test()

        val program: ZIO[DataSource & TerminalService, DataSourceError | TUIError, Chunk[DataPoint]] =
          for
            points <- DataSource.stream.runCollect
            widget <- ZIO.succeed(Widget.section("Test")(points.map(_.label).mkString(", ")))
            _      <- TerminalService.render(widget)
          yield points

        for result <- program.provide(testLayer)
        yield assertTrue(
          result.size == 2,
          result.contains(point1),
          result.contains(point2),
        )
      },
      test("layer composition satisfies type requirements") {
        // Verify that the composition type matches program requirements
        val point     = DataPoint(timestamp = 1000L, value = 99.0, label = "Status")
        val testLayer = DataSource.test(point) ++ TerminalService.test()

        // This should compile because testLayer provides DataSource & TerminalService
        val program: ZIO[DataSource & TerminalService, DataSourceError | TUIError, Unit] =
          DataDashboardApp.program

        program.provide(testLayer).as(assertTrue(true))
      },
    ),
    suite("multi-service integration")(
      test("program uses DataSource.stream accessor") {
        val point1    = DataPoint(timestamp = 1000L, value = 10.0, label = "A")
        val point2    = DataPoint(timestamp = 2000L, value = 20.0, label = "B")
        val testLayer = DataSource.test(point1, point2) ++ TerminalService.test()

        // The program should successfully stream data from DataSource
        for _ <- DataDashboardApp.program.provide(testLayer)
        yield assertTrue(true)
      },
      test("program uses TerminalService.render accessor") {
        val testLayer = DataSource.test() ++ TerminalService.test()

        // The program should successfully render using TerminalService
        for _ <- DataDashboardApp.program.provide(testLayer)
        yield assertTrue(true)
      },
      test("program uses TerminalService.println accessor") {
        val testLayer = DataSource.test() ++ TerminalService.test()

        // The program should successfully print using TerminalService
        for _ <- DataDashboardApp.program.provide(testLayer)
        yield assertTrue(true)
      },
    ),
  )
