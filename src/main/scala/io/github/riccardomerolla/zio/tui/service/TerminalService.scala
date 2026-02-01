package io.github.riccardomerolla.zio.tui.service

import zio.{ IO, UIO, ULayer, ZIO, ZLayer }

import io.github.riccardomerolla.zio.tui.domain.{ Rect, RenderResult, TerminalConfig, Widget }
import io.github.riccardomerolla.zio.tui.error.TUIError

/** Service for managing terminal operations in a resource-safe, effect-typed way.
  *
  * All operations are non-blocking and interruptible. Resources are automatically cleaned up via ZLayer and Scope.
  */
trait TerminalService:
  /** Render a widget to the terminal.
    *
    * @param widget
    *   The widget to render
    * @return
    *   Effect that produces a RenderResult or TUIError
    */
  def render(widget: Widget): IO[TUIError, RenderResult]

  /** Render multiple widgets in sequence.
    *
    * @param widgets
    *   The widgets to render
    * @return
    *   Effect that produces a sequence of RenderResults
    */
  def renderAll(widgets: Seq[Widget]): IO[TUIError, Seq[RenderResult]]

  /** Clear the terminal screen.
    *
    * @return
    *   Effect that clears the terminal
    */
  def clear: IO[TUIError, Unit]

  /** Print raw text to the terminal.
    *
    * @param text
    *   The text to print
    * @return
    *   Effect that prints the text
    */
  def print(text: String): IO[TUIError, Unit]

  /** Print text to the terminal followed by a newline.
    *
    * @param text
    *   The text to print
    * @return
    *   Effect that prints the text with newline
    */
  def println(text: String): IO[TUIError, Unit]

  /** Get current terminal size.
    *
    * @return
    *   Effect that produces terminal dimensions as a Rect
    */
  def size: UIO[Rect]

  /** Flush output buffer to ensure all content is written.
    *
    * @return
    *   Effect that flushes the output stream
    */
  def flush: IO[TUIError, Unit]

  /** Position the cursor at specific coordinates.
    *
    * @param x
    *   Column (0-based)
    * @param y
    *   Row (0-based)
    * @return
    *   Effect that moves the cursor
    */
  def setCursor(x: Int, y: Int): IO[TUIError, Unit]

  /** Hide the terminal cursor.
    *
    * @return
    *   Effect that hides the cursor
    */
  def hideCursor: IO[TUIError, Unit]

  /** Show the terminal cursor.
    *
    * @return
    *   Effect that shows the cursor
    */
  def showCursor: IO[TUIError, Unit]

  /** Enable raw mode (no line buffering, no echo).
    *
    * Must be paired with disableRawMode for cleanup.
    *
    * @return
    *   Effect that enables raw mode
    */
  def enableRawMode: IO[TUIError, Unit]

  /** Disable raw mode, restoring normal terminal behavior.
    *
    * @return
    *   Effect that disables raw mode
    */
  def disableRawMode: IO[TUIError, Unit]

  /** Switch to alternate screen buffer.
    *
    * Must be paired with exitAlternateScreen for cleanup.
    *
    * @return
    *   Effect that switches to alternate screen
    */
  def enterAlternateScreen: IO[TUIError, Unit]

  /** Return to main screen buffer.
    *
    * @return
    *   Effect that returns to main screen
    */
  def exitAlternateScreen: IO[TUIError, Unit]

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
        cause = throwable.getMessage,
      )
    }

  override def renderAll(widgets: Seq[Widget]): IO[TUIError, Seq[RenderResult]] =
    ZIO.foreach(widgets)(render)

  override def clear: IO[TUIError, Unit] =
    ZIO.attempt {
      // ANSI escape code to clear screen
      scala.Console.print("\u001b[2J\u001b[H")
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "clear",
        cause = throwable.getMessage,
      )
    }

  override def print(text: String): IO[TUIError, Unit] =
    ZIO.attempt {
      scala.Console.print(text)
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "print",
        cause = throwable.getMessage,
      )
    }

  override def println(text: String): IO[TUIError, Unit] =
    ZIO.attempt {
      scala.Console.println(text)
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "println",
        cause = throwable.getMessage,
      )
    }

  override def size: UIO[Rect] =
    ZIO.succeed(Rect.fromSize(config.width, config.height))

  override def flush: IO[TUIError, Unit] =
    ZIO.attempt {
      scala.Console.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "flush",
        cause = throwable.getMessage,
      )
    }

  override def setCursor(x: Int, y: Int): IO[TUIError, Unit] =
    ZIO.attempt {
      // ANSI escape code: ESC[{row};{col}H (1-based indexing)
      scala.Console.print(s"\u001b[${y + 1};${x + 1}H")
      scala.Console.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "setCursor",
        cause = throwable.getMessage,
      )
    }

  override def hideCursor: IO[TUIError, Unit] =
    ZIO.attempt {
      // ANSI escape code: ESC[?25l
      scala.Console.print("\u001b[?25l")
      scala.Console.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "hideCursor",
        cause = throwable.getMessage,
      )
    }

  override def showCursor: IO[TUIError, Unit] =
    ZIO.attempt {
      // ANSI escape code: ESC[?25h
      scala.Console.print("\u001b[?25h")
      scala.Console.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "showCursor",
        cause = throwable.getMessage,
      )
    }

  override def enableRawMode: IO[TUIError, Unit] =
    ZIO.attempt {
      // Placeholder - will be implemented with JLine Terminal
      ()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "enableRawMode",
        cause = throwable.getMessage,
      )
    }

  override def disableRawMode: IO[TUIError, Unit] =
    ZIO.attempt {
      // Placeholder - will be implemented with JLine Terminal
      ()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "disableRawMode",
        cause = throwable.getMessage,
      )
    }

  override def enterAlternateScreen: IO[TUIError, Unit] =
    ZIO.attempt {
      // ANSI escape code: ESC[?1049h
      scala.Console.print("\u001b[?1049h")
      scala.Console.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "enterAlternateScreen",
        cause = throwable.getMessage,
      )
    }

  override def exitAlternateScreen: IO[TUIError, Unit] =
    ZIO.attempt {
      // ANSI escape code: ESC[?1049l
      scala.Console.print("\u001b[?1049l")
      scala.Console.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "exitAlternateScreen",
        cause = throwable.getMessage,
      )
    }

object TerminalService:
  /** Access the TerminalService from the environment and render a widget.
    *
    * @param widget
    *   The widget to render
    * @return
    *   Effect requiring TerminalService that produces RenderResult
    */
  def render(widget: Widget): ZIO[TerminalService, TUIError, RenderResult] =
    ZIO.serviceWithZIO[TerminalService](_.render(widget))

  /** Access the TerminalService and print text.
    *
    * @param text
    *   The text to print
    * @return
    *   Effect requiring TerminalService
    */
  def println(text: String): ZIO[TerminalService, TUIError, Unit] =
    ZIO.serviceWithZIO[TerminalService](_.println(text))

  /** Live ZLayer for TerminalService with default configuration.
    */
  val live: ZLayer[Any, Nothing, TerminalService] =
    ZLayer.succeed(TerminalServiceLive(TerminalConfig.default))

  /** Live ZLayer for TerminalService with custom configuration.
    *
    * @param config
    *   The terminal configuration
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

      override def size: UIO[Rect] =
        ZIO.succeed(Rect.fromSize(80, 24))

      override def flush: IO[TUIError, Unit] =
        ZIO.unit

      override def setCursor(x: Int, y: Int): IO[TUIError, Unit] =
        ZIO.unit

      override def hideCursor: IO[TUIError, Unit] =
        ZIO.unit

      override def showCursor: IO[TUIError, Unit] =
        ZIO.unit

      override def enableRawMode: IO[TUIError, Unit] =
        ZIO.unit

      override def disableRawMode: IO[TUIError, Unit] =
        ZIO.unit

      override def enterAlternateScreen: IO[TUIError, Unit] =
        ZIO.unit

      override def exitAlternateScreen: IO[TUIError, Unit] =
        ZIO.unit)

  /** Test/mock implementation returning predefined results.
    *
    * Useful for testing without actual terminal I/O.
    */
  def test(renderResults: Map[Widget, RenderResult] = Map.empty): ULayer[TerminalService] =
    mock(renderResults)
