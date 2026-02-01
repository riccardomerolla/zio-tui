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
final case class TerminalServiceLive(
  config: TerminalConfig,
  terminal: org.jline.terminal.Terminal,
) extends TerminalService:

  override def render(widget: Widget): IO[TUIError, RenderResult] =
    ZIO.attempt {
      val output = widget.render
      terminal.writer().print(output)
      terminal.flush()
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
      val writer = terminal.writer()
      writer.print("\u001b[2J\u001b[H")
      writer.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "clear",
        cause = throwable.getMessage,
      )
    }

  override def print(text: String): IO[TUIError, Unit] =
    ZIO.attempt {
      terminal.writer().print(text)
      terminal.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "print",
        cause = throwable.getMessage,
      )
    }

  override def println(text: String): IO[TUIError, Unit] =
    ZIO.attempt {
      terminal.writer().println(text)
      terminal.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "println",
        cause = throwable.getMessage,
      )
    }

  override def size: UIO[Rect] =
    ZIO.succeed {
      val termSize = terminal.getSize()
      Rect.fromSize(termSize.getColumns(), termSize.getRows())
    }

  override def flush: IO[TUIError, Unit] =
    ZIO.attempt {
      terminal.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "flush",
        cause = throwable.getMessage,
      )
    }

  override def setCursor(x: Int, y: Int): IO[TUIError, Unit] =
    ZIO.attempt {
      val writer = terminal.writer()
      writer.print(s"\u001b[${y + 1};${x + 1}H")
      writer.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "setCursor",
        cause = throwable.getMessage,
      )
    }

  override def hideCursor: IO[TUIError, Unit] =
    ZIO.attempt {
      val writer = terminal.writer()
      writer.print("\u001b[?25l")
      writer.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "hideCursor",
        cause = throwable.getMessage,
      )
    }

  override def showCursor: IO[TUIError, Unit] =
    ZIO.attempt {
      val writer = terminal.writer()
      writer.print("\u001b[?25h")
      writer.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "showCursor",
        cause = throwable.getMessage,
      )
    }

  override def enableRawMode: IO[TUIError, Unit] =
    ZIO.attempt {
      terminal.enterRawMode()
      ()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "enableRawMode",
        cause = throwable.getMessage,
      )
    }

  override def disableRawMode: IO[TUIError, Unit] =
    ZIO.attempt {
      val attributes = terminal.getAttributes()
      attributes.setLocalFlag(org.jline.terminal.Attributes.LocalFlag.ICANON, true)
      attributes.setLocalFlag(org.jline.terminal.Attributes.LocalFlag.ECHO, true)
      terminal.setAttributes(attributes)
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "disableRawMode",
        cause = throwable.getMessage,
      )
    }

  override def enterAlternateScreen: IO[TUIError, Unit] =
    ZIO.attempt {
      val writer = terminal.writer()
      writer.print("\u001b[?1049h")
      writer.flush()
    }.mapError { throwable =>
      TUIError.IOError(
        operation = "enterAlternateScreen",
        cause = throwable.getMessage,
      )
    }

  override def exitAlternateScreen: IO[TUIError, Unit] =
    ZIO.attempt {
      val writer = terminal.writer()
      writer.print("\u001b[?1049l")
      writer.flush()
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

  /** Run an effect with raw mode enabled, automatically disabling on completion.
    *
    * Raw mode disables line buffering and echo, useful for interactive applications. This method ensures raw mode is
    * properly disabled even if the effect fails or is interrupted.
    *
    * @param effect
    *   The effect to run in raw mode
    * @return
    *   Effect that runs with raw mode enabled and automatically cleaned up
    */
  def withRawMode[R, E, A](effect: ZIO[R & TerminalService, E, A]): ZIO[R & TerminalService, E, A] =
    ZIO.acquireReleaseWith(
      ZIO.serviceWithZIO[TerminalService](_.enableRawMode).ignore
    )(_ =>
      ZIO.serviceWithZIO[TerminalService](_.disableRawMode).ignore
    )(_ => effect)

  /** Run an effect in alternate screen buffer, automatically restoring on completion.
    *
    * The alternate screen buffer provides a clean slate separate from the user's terminal history. This method ensures
    * the original screen is restored even if the effect fails or is interrupted.
    *
    * @param effect
    *   The effect to run in alternate screen
    * @return
    *   Effect that runs in alternate screen and automatically restores main screen
    */
  def withAlternateScreen[R, E, A](effect: ZIO[R & TerminalService, E, A]): ZIO[R & TerminalService, E, A] =
    ZIO.acquireReleaseWith(
      ZIO.serviceWithZIO[TerminalService](_.enterAlternateScreen).ignore
    )(_ =>
      ZIO.serviceWithZIO[TerminalService](_.exitAlternateScreen).ignore
    )(_ => effect)

  /** Live ZLayer for TerminalService with default configuration and automatic resource management.
    */
  val live: ZLayer[Any, Nothing, TerminalService] =
    ZLayer.scoped {
      for
        terminal <- ZIO.acquireRelease(
          acquire = ZIO.attempt(
            org.jline.terminal.TerminalBuilder.terminal()
          ).orDie
        )(release = terminal =>
          ZIO.succeed(terminal.close())
        )
      yield TerminalServiceLive(TerminalConfig.default, terminal)
    }

  /** Live ZLayer for TerminalService with custom configuration and automatic resource management.
    *
    * @param config
    *   The terminal configuration
    */
  def withConfig(config: TerminalConfig): ZLayer[Any, Nothing, TerminalService] =
    ZLayer.scoped {
      for
        terminal <- ZIO.acquireRelease(
          acquire = ZIO.attempt(
            org.jline.terminal.TerminalBuilder.terminal()
          ).orDie
        )(release = terminal =>
          ZIO.succeed(terminal.close())
        )
      yield TerminalServiceLive(config, terminal)
    }

  /** Test/mock implementation returning predefined results.
    *
    * Useful for testing without actual terminal I/O.
    *
    * @param terminalSize
    *   The terminal size to report (default 80x24)
    * @param renderResults
    *   Predefined render results for specific widgets
    */
  def test(
    terminalSize: Rect = Rect.fromSize(80, 24),
    renderResults: Map[Widget, RenderResult] = Map.empty,
  ): ULayer[TerminalService] =
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
        ZIO.succeed(terminalSize)

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
        ZIO.unit
    )
