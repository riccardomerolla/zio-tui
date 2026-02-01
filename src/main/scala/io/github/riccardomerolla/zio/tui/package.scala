package io.github.riccardomerolla.zio

/** ZIO-TUI: A ZIO 2.x wrapper for layoutz terminal UI library.
  *
  * This package provides effect-typed interfaces for terminal UI operations, following ZIO best practices:
  *   - Effects as immutable blueprints
  *   - Typed error channels
  *   - Resource-safe operations with ZLayer and Scope
  *   - Composable services via dependency injection
  *
  * ==Quick Start==
  *
  * {{{
  * import io.github.riccardomerolla.zio.tui._
  * import zio._
  *
  * object HelloTUI extends ZIOAppDefault:
  *   def run =
  *     for
  *       terminal <- ZIO.service[TerminalService]
  *       widget   <- ZIO.succeed(Widget.text("Hello, ZIO TUI!"))
  *       result   <- terminal.render(widget)
  *       _        <- terminal.println(result.output)
  *     yield ()
  * }}}
  *
  * ==Core Types==
  *
  *   - [[domain.Widget]]: Immutable widget descriptions
  *   - [[domain.ZTuiApp]]: Core application abstraction for TUI apps
  *   - [[service.TerminalService]]: Main service for terminal operations
  *   - [[error.TUIError]]: Typed errors for terminal operations
  */
package object tui:
  // Re-export commonly used types for convenience
  type Widget = domain.Widget
  val Widget: io.github.riccardomerolla.zio.tui.domain.Widget.type = domain.Widget

  type RenderResult = domain.RenderResult
  val RenderResult: io.github.riccardomerolla.zio.tui.domain.RenderResult.type = domain.RenderResult

  type TerminalConfig = domain.TerminalConfig
  val TerminalConfig: io.github.riccardomerolla.zio.tui.domain.TerminalConfig.type = domain.TerminalConfig

  type Rect = domain.Rect
  val Rect: io.github.riccardomerolla.zio.tui.domain.Rect.type = domain.Rect

  type ZCmd[R, E, Msg] = domain.ZCmd[R, E, Msg]
  val ZCmd: domain.ZCmd.type = domain.ZCmd

  type ZTuiApp[R, E, State, Msg] = domain.ZTuiApp[R, E, State, Msg]

  type TerminalService = service.TerminalService
  val TerminalService: io.github.riccardomerolla.zio.tui.service.TerminalService.type = service.TerminalService

  type TUIError = error.TUIError
  val TUIError: io.github.riccardomerolla.zio.tui.error.TUIError.type = error.TUIError
