package io.github.riccardomerolla.zio.tui.service

import io.github.riccardomerolla.zio.tui.domain.{RenderResult, TerminalConfig, Widget}
import io.github.riccardomerolla.zio.tui.error.TUIError
import zio.{IO, Scope, ULayer, ZIO, ZLayer}

/** Service for managing terminal operations in a resource-safe, effect-typed way.
  *
  * All operations are non-blocking and interruptible.
  * Resources are automatically cleaned up via ZLayer and Scope.
  */
trait TerminalService:
  /** Render a widget to the terminal.
    * 
    * @param widget The widget to render
    * @return Effect that produces a RenderResult or TUIError
    */
  def render(widget: Widget): IO[TUIError, RenderResult]
  
  /** Render multiple widgets in sequence.
    * 
    * @param widgets The widgets to render
    * @return Effect that produces a sequence of RenderResults
    */
  def renderAll(widgets: Seq[Widget]): IO[TUIError, Seq[RenderResult]]
  
  /** Clear the terminal screen.
    * 
    * @return Effect that clears the terminal
    */
  def clear: IO[TUIError, Unit]
  
  /** Print raw text to the terminal.
    * 
    * @param text The text to print
    * @return Effect that prints the text
    */
  def print(text: String): IO[TUIError, Unit]
  
  /** Print text to the terminal followed by a newline.
    * 
    * @param text The text to print
    * @return Effect that prints the text with newline
    */
  def println(text: String): IO[TUIError, Unit]

/** Live implementation of TerminalService using stdout.
  */
final case class TerminalServiceLive(config: TerminalConfig) extends TerminalService:
  
  override def render(widget: Widget): IO[TUIError, RenderResult] =
    ZIO.attempt {
      val output = widget.render
      RenderResult.fromString(output)
    }.mapError { throwable =>
      TUIError.RenderingFailed(
        widget = widget.element.getClass.getSimpleName,
        cause = throwable.getMessage
      )
    }
  
  override def renderAll(widgets: Seq[Widget]): IO[TUIError, Seq[RenderResult]] =
    ZIO.foreachPar(widgets)(render)
  
  override def clear: IO[TUIError, Unit] =
    ZIO.attempt {
      // ANSI escape code to clear screen
      scala.Console.print("\u001b[2J\u001b[H")
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "clear",
        cause = throwable.getMessage
      )
    }
  
  override def print(text: String): IO[TUIError, Unit] =
    ZIO.attempt {
      scala.Console.print(text)
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "print",
        cause = throwable.getMessage
      )
    }
  
  override def println(text: String): IO[TUIError, Unit] =
    ZIO.attempt {
      scala.Console.println(text)
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "println",
        cause = throwable.getMessage
      )
    }

object TerminalService:
  /** Access the TerminalService from the environment and render a widget.
    * 
    * @param widget The widget to render
    * @return Effect requiring TerminalService that produces RenderResult
    */
  def render(widget: Widget): ZIO[TerminalService, TUIError, RenderResult] =
    ZIO.serviceWithZIO[TerminalService](_.render(widget))
  
  /** Access the TerminalService and print text.
    * 
    * @param text The text to print
    * @return Effect requiring TerminalService
    */
  def println(text: String): ZIO[TerminalService, TUIError, Unit] =
    ZIO.serviceWithZIO[TerminalService](_.println(text))
  
  /** Live ZLayer for TerminalService with default configuration.
    */
  val live: ZLayer[Any, Nothing, TerminalService] =
    ZLayer.succeed(TerminalServiceLive(TerminalConfig.default))
  
  /** Live ZLayer for TerminalService with custom configuration.
    * 
    * @param config The terminal configuration
    */
  def withConfig(config: TerminalConfig): ULayer[TerminalService] =
    ZLayer.succeed(TerminalServiceLive(config))
  
  /** Test/mock implementation returning predefined results.
    * 
    * Useful for testing without actual terminal I/O.
    */
  def mock(renderResults: Map[Widget, RenderResult] = Map.empty): ULayer[TerminalService] =
    ZLayer.succeed(new TerminalService:
      override def render(widget: Widget): IO[TUIError, RenderResult] =
        ZIO.succeed(
          renderResults.getOrElse(widget, RenderResult.fromString(widget.render))
        )
      
      override def renderAll(widgets: Seq[Widget]): IO[TUIError, Seq[RenderResult]] =
        ZIO.foreach(widgets)(render)
      
      override def clear: IO[TUIError, Unit] =
        ZIO.unit
      
      override def print(text: String): IO[TUIError, Unit] =
        ZIO.unit
      
      override def println(text: String): IO[TUIError, Unit] =
        ZIO.unit
    )
