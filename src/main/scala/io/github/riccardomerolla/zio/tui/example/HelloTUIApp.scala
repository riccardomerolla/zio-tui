package io.github.riccardomerolla.zio.tui.example

import io.github.riccardomerolla.zio.tui._
import zio.{ZIO, ZIOAppDefault}

/** Example application demonstrating zio-tui capabilities.
  *
  * This application showcases:
  * - Widget creation (text, sections, lists, tables)
  * - ZLayer composition and dependency injection
  * - Effect-typed terminal operations
  * - Typed error handling
  * - Resource-safe operations
  */
object HelloTUIApp extends ZIOAppDefault:
  
  /** Demonstrate text widgets. */
  def textDemo: ZIO[TerminalService, TUIError, Unit] =
    for
      _ <- TerminalService.println("\n=== Text Widget Demo ===")
      widget <- ZIO.succeed(Widget.text("Hello, ZIO TUI!"))
      result <- TerminalService.render(widget)
      _ <- TerminalService.println(result.output)
    yield ()
  
  /** Demonstrate section widgets. */
  def sectionDemo: ZIO[TerminalService, TUIError, Unit] =
    for
      _ <- TerminalService.println("\n=== Section Widget Demo ===")
      widget <- ZIO.succeed(
        Widget.section("Configuration")(
          "Environment: Production\nVersion: 1.0.0"
        )
      )
      result <- TerminalService.render(widget)
      _ <- TerminalService.println(result.output)
    yield ()
  
  /** Demonstrate list widgets. */
  def listDemo: ZIO[TerminalService, TUIError, Unit] =
    for
      _ <- TerminalService.println("\n=== List Widget Demo ===")
      widget <- ZIO.succeed(
        Widget.list(
          "Effect-typed operations",
          "ZIO Ecosystem integration",
          "Type-safe resource management",
          "Fiber-based concurrency"
        )
      )
      result <- TerminalService.render(widget)
      _ <- TerminalService.println(result.output)
    yield ()
  
  /** Demonstrate table widgets. */
  def tableDemo: ZIO[TerminalService, TUIError, Unit] =
    for
      _ <- TerminalService.println("\n=== Table Widget Demo ===")
      widget <- ZIO.succeed(
        Widget.table(
          Seq("Service", "Status", "Uptime"),
          Seq(
            Seq("API", "✓ Running", "99.9%"),
            Seq("Database", "✓ Running", "99.8%"),
            Seq("Cache", "✓ Running", "100%")
          )
        )
      )
      result <- TerminalService.render(widget)
      _ <- TerminalService.println(result.output)
    yield ()
  
  /** Demonstrate error handling. */
  def errorHandlingDemo: ZIO[TerminalService, TUIError, Unit] =
    for
      _ <- TerminalService.println("\n=== Error Handling Demo ===")
      _ <- TerminalService.println("All operations are typed with TUIError")
      _ <- TerminalService.println("Errors are explicit in the type signature")
      _ <- TerminalService.println("No thrown exceptions - only ZIO effects")
    yield ()
  
  /** Main application logic. */
  def program: ZIO[TerminalService, TUIError, Unit] =
    for
      _ <- TerminalService.println("╔════════════════════════════════════════╗")
      _ <- TerminalService.println("║     ZIO-TUI Demo Application          ║")
      _ <- TerminalService.println("║  A ZIO 2.x wrapper for layoutz        ║")
      _ <- TerminalService.println("╚════════════════════════════════════════╝")
      _ <- textDemo
      _ <- sectionDemo
      _ <- listDemo
      _ <- tableDemo
      _ <- errorHandlingDemo
      _ <- TerminalService.println("\n✨ Demo completed successfully!")
    yield ()
  
  /** Application entry point with dependency injection. */
  def run =
    program
      .provide(TerminalService.live)
      .catchAll { error =>
        ZIO.logError(s"Application failed with error: $error") *>
        ZIO.fail(error)
      }
      .exitCode
