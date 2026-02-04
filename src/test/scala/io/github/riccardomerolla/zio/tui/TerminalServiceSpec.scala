package io.github.riccardomerolla.zio.tui

import java.io.*
import java.nio.charset.Charset
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

import zio.test.{ Spec, TestEnvironment, ZIOSpecDefault, assertTrue }
import zio.{ Scope, ZEnvironment, ZIO, ZLayer }

import io.github.riccardomerolla.zio.tui.domain.{ Rect, RenderResult, TerminalConfig, Widget }
import io.github.riccardomerolla.zio.tui.error.TUIError
import io.github.riccardomerolla.zio.tui.service.{ TerminalService, TerminalServiceLive }
import org.jline.terminal.impl.AbstractTerminal
import org.jline.terminal.spi.{ SystemStream, TerminalProvider }
import org.jline.terminal.{ Attributes, Size }
import org.jline.utils.{ NonBlockingReader, NonBlockingReaderImpl }

/** ZIO Test specification for TerminalService.
  *
  * Tests cover:
  *   - Widget rendering with success and failure paths
  *   - Render result correctness
  *   - Error handling with typed errors
  *   - Service layer composition
  *   - TerminalServiceLive implementation
  */
object TerminalServiceSpec extends ZIOSpecDefault:

  // Test terminal that captures output for verification
  class TestTerminal extends AbstractTerminal("test", "ansi"):
    private val outputBuffer      = new StringWriter()
    private val testWriter        = new PrintWriter(outputBuffer)
    private val inputStream       = new ByteArrayInputStream(Array.emptyByteArray)
    private val outputStream      = new ByteArrayOutputStream()
    private val nonBlockingReader =
      new NonBlockingReaderImpl("test", new java.io.InputStreamReader(inputStream, Charset.defaultCharset()))
    private val attributes        = new AtomicReference[Attributes](new Attributes())
    private val terminalSize      = new AtomicReference[Size](new Size(80, 24))
    private val rawModeEnabled    = new AtomicBoolean(false)

    override def writer(): PrintWriter = testWriter

    override def getSize(): Size = terminalSize.get()

    def setSize(cols: Int, rows: Int): Unit =
      terminalSize.set(new Size(cols, rows))

    def getOutput: String = outputBuffer.toString

    def clearOutput(): Unit =
      outputBuffer.getBuffer.setLength(0)

    override def getAttributes(): Attributes = attributes.get()

    override def setAttributes(attrs: Attributes): Unit =
      attributes.set(attrs)

    override def input(): InputStream = inputStream

    override def output(): OutputStream = outputStream

    override def reader(): NonBlockingReader = nonBlockingReader

    override def setSize(size: Size): Unit =
      terminalSize.set(size)

    override def getProvider(): TerminalProvider =
      TestTerminal.provider

    override def getSystemStream(): SystemStream =
      SystemStream.Output

    override def enterRawMode(): Attributes =
      rawModeEnabled.set(true)
      getAttributes()

    def isRawModeEnabled: Boolean = rawModeEnabled.get()

  object TestTerminal:
    private val provider: TerminalProvider = new TerminalProvider:
      override def name(): String = "test"

      override def sysTerminal(
        name: String,
        `type`: String,
        ansiPassThrough: Boolean,
        encoding: Charset,
        nativeSignals: Boolean,
        signalHandler: org.jline.terminal.Terminal.SignalHandler,
        paused: Boolean,
        stream: SystemStream,
      ): org.jline.terminal.Terminal =
        new TestTerminal

      override def newTerminal(
        name: String,
        `type`: String,
        in: InputStream,
        out: OutputStream,
        encoding: Charset,
        signalHandler: org.jline.terminal.Terminal.SignalHandler,
        paused: Boolean,
        attributes: Attributes,
        size: Size,
      ): org.jline.terminal.Terminal =
        new TestTerminal

      override def isSystemStream(stream: SystemStream): Boolean = false

      override def systemStreamName(stream: SystemStream): String = "test"

      override def systemStreamWidth(stream: SystemStream): Int = 0

  // Helper to create a test layer with a TestTerminal
  def testTerminalLayer: ZLayer[Any, Nothing, (TerminalService, TestTerminal)] =
    ZLayer.scoped {
      for
        terminal <- ZIO.succeed(new TestTerminal)
        service   = TerminalServiceLive(TerminalConfig.default, terminal)
        _        <- ZIO.addFinalizer(ZIO.succeed(terminal.close()))
      yield (service, terminal)
    }

  // Extract just the service
  private val liveServiceLayer: ZLayer[Any, Nothing, TerminalService] =
    testTerminalLayer.map { env =>
      ZEnvironment(env.get[(TerminalService, TestTerminal)]._1)
    }

  private val testLayer: ZLayer[Any, Nothing, TerminalService] =
    TerminalService.test()

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("TerminalService")(
    suite("rendering")(
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
                      Seq("Name", "Age").map(layoutz.Text(_)),
                      Seq(
                        Seq("Alice", "30"),
                        Seq("Bob", "25"),
                      ).map(_.map(layoutz.Text(_))),
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
      test("renderAll renders multiple widgets") {
        for
          service <- ZIO.service[TerminalService]
          widgets <- ZIO.succeed(Seq(Widget.text("First"), Widget.text("Second")))
          results <- service.renderAll(widgets)
        yield assertTrue(
          results.length == 2,
          results(0).output.contains("First"),
          results(1).output.contains("Second"),
        )
      },
      test("clear succeeds without error") {
        for
          service <- ZIO.service[TerminalService]
          _       <- service.clear
        yield assertTrue(true)
      },
      test("print outputs text") {
        for
          service <- ZIO.service[TerminalService]
          _       <- service.print("No newline")
        yield assertTrue(true)
      },
    ),
    suite("domain types")(
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
    ),
    suite("terminal operations")(
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
    ),
    suite("scoped helpers")(
      test("withRawMode enables and disables raw mode") {
        for
          result <- TerminalService.withRawMode {
                      ZIO.succeed("executed in raw mode")
                    }
        yield assertTrue(result == "executed in raw mode")
      }.provideLayer(TerminalService.test()),
      test("withAlternateScreen enters and exits alternate screen") {
        for
          result <- TerminalService.withAlternateScreen {
                      ZIO.succeed("executed in alternate screen")
                    }
        yield assertTrue(result == "executed in alternate screen")
      }.provideLayer(TerminalService.test()),
      test("withRawMode and withAlternateScreen compose") {
        for
          result <- TerminalService.withRawMode {
                      TerminalService.withAlternateScreen {
                        ZIO.succeed("nested scoped effects")
                      }
                    }
        yield assertTrue(result == "nested scoped effects")
      }.provideLayer(TerminalService.test()),
    ),
    suite("test layer configuration")(
      test("test layer with custom terminal size") {
        for
          size <- ZIO.serviceWithZIO[TerminalService](_.size)
        yield assertTrue(
          size.width == 100,
          size.height == 50,
        )
      }.provideLayer(TerminalService.test(terminalSize = Rect.fromSize(100, 50))),
      test("test layer with default size") {
        for
          size <- ZIO.serviceWithZIO[TerminalService](_.size)
        yield assertTrue(
          size.width == 80,
          size.height == 24,
        )
      }.provideLayer(TerminalService.test()),
      test("test layer with custom render results") {
        val widget = Widget.text("custom")
        RenderResult.fromString("Custom Output")
        for
          result <- TerminalService.render(widget)
        yield assertTrue(result.output == "Custom Output")
      }.provideLayer(TerminalService.test(renderResults =
        Map(Widget.text("custom") -> RenderResult.fromString("Custom Output"))
      )),
    ),
    suite("accessor methods")(
      test("render accessor method") {
        for
          widget <- ZIO.succeed(Widget.text("Accessor test"))
          result <- TerminalService.render(widget)
        yield assertTrue(result.output.contains("Accessor test"))
      },
      test("println accessor method") {
        for
          _ <- TerminalService.println("Accessor println")
        yield assertTrue(true)
      },
    ),
    suite("edge cases")(
      test("render empty text") {
        for
          widget <- ZIO.succeed(Widget.text(""))
          result <- TerminalService.render(widget)
        yield assertTrue(result.output.isEmpty)
      },
      test("renderAll with empty sequence") {
        for
          service <- ZIO.service[TerminalService]
          results <- service.renderAll(Seq.empty)
        yield assertTrue(results.isEmpty)
      },
      test("renderAll with single widget") {
        for
          service <- ZIO.service[TerminalService]
          widgets <- ZIO.succeed(Seq(Widget.text("Only one")))
          results <- service.renderAll(widgets)
        yield assertTrue(
          results.length == 1,
          results.head.output.contains("Only one"),
        )
      },
      test("setCursor with boundary coordinates") {
        for
          _ <- ZIO.serviceWithZIO[TerminalService](_.setCursor(0, 0))
          _ <- ZIO.serviceWithZIO[TerminalService](_.setCursor(79, 23))
        yield assertTrue(true)
      }.provideLayer(TerminalService.test()),
      test("multiple operations in sequence") {
        for
          service <- ZIO.service[TerminalService]
          _       <- service.clear
          _       <- service.hideCursor
          _       <- service.setCursor(10, 10)
          widget  <- ZIO.succeed(Widget.text("Test"))
          _       <- service.render(widget)
          _       <- service.showCursor
        yield assertTrue(true)
      }.provideLayer(TerminalService.test()),
    ),
    suite("TerminalServiceLive")(
      test("render widget writes to terminal") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          widget              <- ZIO.succeed(Widget.text("Hello Live!"))
          _                   <- service.render(widget)
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("Hello Live!")
        )
      }.provideLayer(testTerminalLayer),
      test("render returns correct RenderResult") {
        for
          service <- ZIO.service[TerminalService]
          widget  <- ZIO.succeed(Widget.text("Test Output"))
          result  <- service.render(widget)
        yield assertTrue(
          result.output == "Test Output",
          result.charCount == 11,
          result.lineCount == 1,
        )
      }.provideLayer(liveServiceLayer),
      test("renderAll processes multiple widgets") {
        for
          service <- ZIO.service[TerminalService]
          widgets  = Seq(Widget.text("First"), Widget.text("Second"), Widget.text("Third"))
          results <- service.renderAll(widgets)
        yield assertTrue(
          results.length == 3,
          results(0).output == "First",
          results(1).output == "Second",
          results(2).output == "Third",
        )
      }.provideLayer(liveServiceLayer),
      test("clear writes ANSI clear sequence") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                   <- service.clear
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[2J\u001b[H")
        )
      }.provideLayer(testTerminalLayer),
      test("print writes text without newline") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.print("No newline")
          output               = terminal.getOutput
        yield assertTrue(
          output == "No newline",
          !output.contains("\n"),
        )
      }.provideLayer(testTerminalLayer),
      test("println writes text with newline") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.println("With newline")
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("With newline"),
          output.endsWith("\n"),
        )
      }.provideLayer(testTerminalLayer),
      test("size returns terminal dimensions") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.setSize(100, 50)
          size                <- service.size
        yield assertTrue(
          size.width == 100,
          size.height == 50,
          size.x == 0,
          size.y == 0,
        )
      }.provideLayer(testTerminalLayer),
      test("flush completes without error") {
        for
          service <- ZIO.service[TerminalService]
          _       <- service.flush
        yield assertTrue(true)
      }.provideLayer(liveServiceLayer),
      test("setCursor writes ANSI cursor position sequence") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.setCursor(10, 5)
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[6;11H") // ANSI uses 1-based indexing
        )
      }.provideLayer(testTerminalLayer),
      test("hideCursor writes ANSI hide cursor sequence") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.hideCursor
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[?25l")
        )
      }.provideLayer(testTerminalLayer),
      test("showCursor writes ANSI show cursor sequence") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.showCursor
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[?25h")
        )
      }.provideLayer(testTerminalLayer),
      test("enableRawMode activates raw mode") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                   <- service.enableRawMode
        yield assertTrue(
          terminal.isRawModeEnabled
        )
      }.provideLayer(testTerminalLayer),
      test("disableRawMode completes without error") {
        for
          service <- ZIO.service[TerminalService]
          _       <- service.enableRawMode
          _       <- service.disableRawMode
        yield assertTrue(true)
      }.provideLayer(liveServiceLayer),
      test("enterAlternateScreen writes ANSI alternate screen sequence") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.enterAlternateScreen
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[?1049h")
        )
      }.provideLayer(testTerminalLayer),
      test("exitAlternateScreen writes ANSI exit alternate screen sequence") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.exitAlternateScreen
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[?1049l")
        )
      }.provideLayer(testTerminalLayer),
      test("multiple operations write to same terminal") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.clear
          _                   <- service.hideCursor
          _                   <- service.setCursor(5, 10)
          _                   <- service.print("Test")
          _                   <- service.showCursor
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[2J\u001b[H"), // clear
          output.contains("\u001b[?25l"),       // hide cursor
          output.contains("\u001b[11;6H"),      // setCursor
          output.contains("Test"),              // print
          output.contains("\u001b[?25h"), // show cursor
        )
      }.provideLayer(testTerminalLayer),
      test("render widget flushes output") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          widget               = Widget.text("Flushed")
          _                   <- service.render(widget)
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("Flushed")
        )
      }.provideLayer(testTerminalLayer),
      test("renderAll renders widgets in order") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          widgets              = Seq(
                                   Widget.text("First"),
                                   Widget.text("Second"),
                                   Widget.text("Third"),
                                 )
          _                   <- service.renderAll(widgets)
          output               = terminal.getOutput
          firstIdx             = output.indexOf("First")
          secondIdx            = output.indexOf("Second")
          thirdIdx             = output.indexOf("Third")
        yield assertTrue(
          firstIdx >= 0,
          secondIdx >= 0,
          thirdIdx >= 0,
          firstIdx < secondIdx,
          secondIdx < thirdIdx,
        )
      }.provideLayer(testTerminalLayer),
      test("size reflects terminal resize") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          initialSize         <- service.size
          _                    = terminal.setSize(120, 40)
          newSize             <- service.size
        yield assertTrue(
          initialSize.width == 80,
          initialSize.height == 24,
          newSize.width == 120,
          newSize.height == 40,
        )
      }.provideLayer(testTerminalLayer),
      test("setCursor with zero coordinates") {
        for
          (service, terminal) <- ZIO.service[(TerminalService, TestTerminal)]
          _                    = terminal.clearOutput()
          _                   <- service.setCursor(0, 0)
          output               = terminal.getOutput
        yield assertTrue(
          output.contains("\u001b[1;1H") // 1-based indexing
        )
      }.provideLayer(testTerminalLayer),
      test("render empty widget") {
        for
          service <- ZIO.service[TerminalService]
          widget   = Widget.text("")
          result  <- service.render(widget)
        yield assertTrue(
          result.output.isEmpty,
          result.charCount == 0,
        )
      }.provideLayer(liveServiceLayer),
      test("renderAll with empty sequence") {
        for
          service <- ZIO.service[TerminalService]
          results <- service.renderAll(Seq.empty)
        yield assertTrue(
          results.isEmpty
        )
      }.provideLayer(liveServiceLayer),
    ),
  ).provideLayerShared(testLayer)
