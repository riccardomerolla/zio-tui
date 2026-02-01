package io.github.riccardomerolla.zio

/** ZIO-TUI: A ZIO 2.x wrapper for layoutz terminal UI library.
  * 
  * This package provides effect-typed interfaces for terminal UI operations,
  * following ZIO best practices:
  * - Effects as immutable blueprints
  * - Typed error channels
  * - Resource-safe operations with ZLayer and Scope
  * - Composable services via dependency injection
  * 
  * == Quick Start ==
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
  * == Core Types ==
  * 
  * - [[domain.Widget]]: Immutable widget descriptions
  * - [[service.TerminalService]]: Main service for terminal operations
  * - [[error.TUIError]]: Typed errors for terminal operations
  */
package object tui:
  // Re-export commonly used types for convenience
  type Widget = domain.Widget
  val Widget = domain.Widget
  
  type RenderResult = domain.RenderResult
  val RenderResult = domain.RenderResult
  
  type TerminalConfig = domain.TerminalConfig
  val TerminalConfig = domain.TerminalConfig
  
  type TerminalService = service.TerminalService
  val TerminalService = service.TerminalService
  
  type TUIError = error.TUIError
  val TUIError = error.TUIError
