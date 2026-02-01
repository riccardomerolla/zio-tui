package io.github.riccardomerolla.zio.tui

import zio.test.{ Spec, TestEnvironment, ZIOSpecDefault, assertTrue }
import zio.{ Scope, ZIO, ZLayer }

import io.github.riccardomerolla.zio.tui.domain.{ RenderResult, Widget }
import io.github.riccardomerolla.zio.tui.error.TUIError
import io.github.riccardomerolla.zio.tui.service.TerminalService

/** ZIO Test specification for TerminalService.
  *
  * Tests cover:
  *   - Widget rendering with success and failure paths
  *   - Render result correctness
  *   - Error handling with typed errors
  *   - Service layer composition
  */
object TerminalServiceSpec extends ZIOSpecDefault:

  private val testLayer: ZLayer[Any, Nothing, TerminalService] =
    TerminalService.mock()

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("TerminalService")(
    test("renders text widget successfully") {
      for
        widget <- ZIO.succeed(Widget.text("Hello, ZIO TUI!"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Hello, ZIO TUI!"),
        result.charCount > 0,
      )
    },
    test("renders section widget with title") {
      for
        widget <- ZIO.succeed(Widget.section("Test Section")("Content here"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Test Section"),
        result.output.contains("Content here"),
      )
    },
    test("renders list widget with multiple items") {
      for
        widget <- ZIO.succeed(Widget.list("Item 1", "Item 2", "Item 3"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Item 1"),
        result.output.contains("Item 2"),
        result.output.contains("Item 3"),
      )
    },
    test("renders table widget with headers and rows") {
      for
        widget <- ZIO.succeed(Widget.table(
                    Seq("Name", "Age"),
                    Seq(
                      Seq("Alice", "30"),
                      Seq("Bob", "25"),
                    ),
                  ))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Name"),
        result.output.contains("Age"),
        result.output.contains("Alice"),
        result.output.contains("Bob"),
      )
    },
    test("render result contains correct metadata") {
      for
        widget <- ZIO.succeed(Widget.text("Test"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.lineCount >= 1,
        result.charCount >= 4,
        result.output == "Test",
      )
    },
    test("println outputs text without failure") {
      for
        _ <- TerminalService.println("Test output")
      yield assertTrue(true)
    },
    test("Rect represents terminal dimensions") {
      val rect = Rect.fromSize(80, 24)
      assertTrue(
        rect.x == 0,
        rect.y == 0,
        rect.width == 80,
        rect.height == 24,
      )
    },
    test("Rect can be created with position") {
      val rect = Rect(x = 10, y = 5, width = 60, height = 15)
      assertTrue(
        rect.x == 10,
        rect.y == 5,
        rect.width == 60,
        rect.height == 15,
      )
    },
    test("size returns terminal dimensions") {
      for
        size <- ZIO.serviceWithZIO[TerminalService](_.size)
      yield assertTrue(
        size.width == 80,
        size.height == 24,
        size.x == 0,
        size.y == 0,
      )
    }.provideLayer(TerminalService.test()),
    test("flush completes without error") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.flush)
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
    test("setCursor positions cursor at coordinates") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.setCursor(10, 5))
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
    test("hideCursor succeeds") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.hideCursor)
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
    test("showCursor succeeds") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.showCursor)
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
    test("enableRawMode succeeds") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.enableRawMode)
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
    test("disableRawMode succeeds") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.disableRawMode)
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
    test("enterAlternateScreen succeeds") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.enterAlternateScreen)
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
    test("exitAlternateScreen succeeds") {
      for
        _ <- ZIO.serviceWithZIO[TerminalService](_.exitAlternateScreen)
      yield assertTrue(true)
    }.provideLayer(TerminalService.test()),
  ).provideLayerShared(testLayer)
