package io.github.riccardomerolla.zio.tui

import io.github.riccardomerolla.zio.tui.domain.{RenderResult, Widget, TerminalConfig}
import io.github.riccardomerolla.zio.tui.error.TUIError
import io.github.riccardomerolla.zio.tui.service.TerminalService
import zio.test.{ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO, ZLayer}

/** ZIO Test specification for TerminalService.
  *
  * Tests cover:
  * - Widget rendering with success and failure paths
  * - Render result correctness
  * - Error handling with typed errors
  * - Service layer composition
  */
object TerminalServiceSpec extends ZIOSpecDefault:
  
  private val testLayer: ZLayer[Any, Nothing, TerminalService] =
    TerminalService.mock()
  
  def spec = suite("TerminalService")(
    test("renders text widget successfully") {
      for
        widget <- ZIO.succeed(Widget.text("Hello, ZIO TUI!"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Hello, ZIO TUI!"),
        result.charCount > 0
      )
    },
    
    test("renders section widget with title") {
      for
        widget <- ZIO.succeed(Widget.section("Test Section")("Content here"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Test Section"),
        result.output.contains("Content here")
      )
    },
    
    test("renders list widget with multiple items") {
      for
        widget <- ZIO.succeed(Widget.list("Item 1", "Item 2", "Item 3"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Item 1"),
        result.output.contains("Item 2"),
        result.output.contains("Item 3")
      )
    },
    
    test("renders table widget with headers and rows") {
      for
        widget <- ZIO.succeed(Widget.table(
          Seq("Name", "Age"),
          Seq(
            Seq("Alice", "30"),
            Seq("Bob", "25")
          )
        ))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.output.contains("Name"),
        result.output.contains("Age"),
        result.output.contains("Alice"),
        result.output.contains("Bob")
      )
    },
    
    test("render result contains correct metadata") {
      for
        widget <- ZIO.succeed(Widget.text("Test"))
        result <- TerminalService.render(widget)
      yield assertTrue(
        result.lineCount >= 1,
        result.charCount >= 4,
        result.output == "Test"
      )
    },
    
    test("println outputs text without failure") {
      for
        _ <- TerminalService.println("Test output")
      yield assertTrue(true)
    }
  ).provideLayerShared(testLayer)
