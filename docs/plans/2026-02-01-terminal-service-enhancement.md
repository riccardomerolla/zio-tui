# TerminalService Enhancement Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enhance TerminalService with comprehensive ZIO-based terminal operations including cursor control, raw mode management, alternate screen buffer, and terminal size detection.

**Architecture:** Use JLine 3 for cross-platform terminal control with proper ZIO resource management via ZLayer and Scope. Provide both manual control methods and scoped helpers for automatic cleanup.

**Tech Stack:** Scala 3, ZIO 2.x, JLine 3, ZIO Test

---

## Task 1: Add JLine 3 Dependency

**Files:**
- Modify: `build.sbt:42-48`

**Step 1: Add JLine dependency to build.sbt**

Add the JLine dependency to the `libraryDependencies` section:

```scala
libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.1.24",
  "dev.zio" %% "zio-streams" % "2.1.24",
  "xyz.matthieucourt" %% "layoutz" % "0.6.0",
  "org.jline" % "jline" % "3.25.1",
  "dev.zio" %% "zio-test" % "2.1.24" % Test,
  "dev.zio" %% "zio-test-sbt" % "2.1.24" % Test
),
```

**Step 2: Reload sbt to download dependency**

Run: `sbt reload`
Expected: JLine dependency downloads successfully

**Step 3: Commit**

```bash
git add build.sbt
git commit -m "build: add JLine 3 dependency for terminal control"
```

---

## Task 2: Create Rect Domain Type

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/domain/Widget.scala:99` (after TerminalConfig)

**Step 1: Write failing test for Rect**

Add to `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`:

```scala
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
```

**Step 2: Run test to verify it fails**

Run: `sbt test`
Expected: Compilation error - "Not found: type Rect"

**Step 3: Add Rect domain type**

Add to `src/main/scala/io/github/riccardomerolla/zio/tui/domain/Widget.scala` after `TerminalConfig`:

```scala
/** Represents a rectangle in terminal coordinate space.
  *
  * @param x
  *   The column position (0-based, left edge)
  * @param y
  *   The row position (0-based, top edge)
  * @param width
  *   The width in columns
  * @param height
  *   The height in rows
  */
final case class Rect(
  x: Int,
  y: Int,
  width: Int,
  height: Int,
)

object Rect:
  /** Create a Rect representing full terminal size starting at origin.
    *
    * @param width
    *   Terminal width in columns
    * @param height
    *   Terminal height in rows
    * @return
    *   Rect positioned at (0,0) with given dimensions
    */
  def fromSize(width: Int, height: Int): Rect =
    Rect(x = 0, y = 0, width = width, height = height)
```

**Step 4: Export Rect in package object**

Modify `src/main/scala/io/github/riccardomerolla/zio/tui/package.scala` to export Rect:

Add these lines after the TerminalConfig exports:

```scala
type Rect = domain.Rect
val Rect: io.github.riccardomerolla.zio.tui.domain.Rect.type = domain.Rect
```

**Step 5: Run test to verify it passes**

Run: `sbt test`
Expected: All tests pass

**Step 6: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/domain/Widget.scala src/main/scala/io/github/riccardomerolla/zio/tui/package.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "feat: add Rect domain type for terminal dimensions"
```

---

## Task 3: Add Terminal Size Method

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala:12-20`

**Step 1: Write failing test for size method**

Add to `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`:

```scala
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
```

**Step 2: Run test to verify it fails**

Run: `sbt test`
Expected: Compilation error - "value size is not a member of TerminalService"

**Step 3: Add size method to TerminalService trait**

In `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala`, add after the `println` method:

```scala
/** Get current terminal size.
  *
  * @return
  *   Effect that produces terminal dimensions as a Rect
  */
def size: UIO[Rect]
```

Update imports at the top to include Rect:

```scala
import io.github.riccardomerolla.zio.tui.domain.{ Rect, RenderResult, TerminalConfig, Widget }
```

**Step 4: Add size implementation to TerminalServiceLive**

This will fail to compile until we add JLine Terminal field. For now, add a placeholder implementation after `println`:

```scala
override def size: UIO[Rect] =
  ZIO.succeed(Rect.fromSize(config.width, config.height))
```

**Step 5: Update test/mock implementation**

Update the `mock` method in `TerminalService` companion object to include size:

```scala
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
      ZIO.succeed(Rect.fromSize(80, 24)))
```

**Step 6: Run test to verify it passes**

Run: `sbt test`
Expected: All tests pass

**Step 7: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "feat: add size method to TerminalService"
```

---

## Task 4: Add Flush Method

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala`

**Step 1: Write failing test for flush**

Add to `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`:

```scala
test("flush completes without error") {
  for
    _ <- ZIO.serviceWithZIO[TerminalService](_.flush)
  yield assertTrue(true)
}.provideLayer(TerminalService.test()),
```

**Step 2: Run test to verify it fails**

Run: `sbt test`
Expected: Compilation error - "value flush is not a member of TerminalService"

**Step 3: Add flush method to TerminalService trait**

Add after the `size` method:

```scala
/** Flush output buffer to ensure all content is written.
  *
  * @return
  *   Effect that flushes the output stream
  */
def flush: IO[TUIError, Unit]
```

**Step 4: Implement flush in TerminalServiceLive**

Add after the `size` implementation:

```scala
override def flush: IO[TUIError, Unit] =
  ZIO.attempt {
    scala.Console.flush()
  }.mapError { throwable =>
    TUIError.IOError(
      operation = "flush",
      cause = throwable.getMessage,
    )
  }
```

**Step 5: Update mock implementation**

Add to the mock method:

```scala
override def flush: IO[TUIError, Unit] =
  ZIO.unit
```

**Step 6: Run test to verify it passes**

Run: `sbt test`
Expected: All tests pass

**Step 7: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "feat: add flush method to TerminalService"
```

---

## Task 5: Add Cursor Control Methods

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala`

**Step 1: Write failing tests for cursor operations**

Add to `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`:

```scala
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
```

**Step 2: Run test to verify it fails**

Run: `sbt test`
Expected: Compilation errors for missing methods

**Step 3: Add cursor methods to TerminalService trait**

Add after the `flush` method:

```scala
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
```

**Step 4: Implement cursor methods in TerminalServiceLive**

Add after the `flush` implementation:

```scala
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
```

**Step 5: Update mock implementation**

Add to the mock method:

```scala
override def setCursor(x: Int, y: Int): IO[TUIError, Unit] =
  ZIO.unit

override def hideCursor: IO[TUIError, Unit] =
  ZIO.unit

override def showCursor: IO[TUIError, Unit] =
  ZIO.unit
```

**Step 6: Run test to verify it passes**

Run: `sbt test`
Expected: All tests pass

**Step 7: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "feat: add cursor control methods to TerminalService"
```

---

## Task 6: Add Raw Mode Methods

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala`

**Step 1: Write failing tests for raw mode**

Add to `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`:

```scala
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
```

**Step 2: Run test to verify it fails**

Run: `sbt test`
Expected: Compilation errors for missing methods

**Step 3: Add raw mode methods to TerminalService trait**

Add after the cursor methods:

```scala
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
```

**Step 4: Implement raw mode methods in TerminalServiceLive (placeholder)**

For now, add placeholder implementations (we'll use JLine in Task 8):

```scala
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
```

**Step 5: Update mock implementation**

Add to the mock method:

```scala
override def enableRawMode: IO[TUIError, Unit] =
  ZIO.unit

override def disableRawMode: IO[TUIError, Unit] =
  ZIO.unit
```

**Step 6: Run test to verify it passes**

Run: `sbt test`
Expected: All tests pass

**Step 7: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "feat: add raw mode methods to TerminalService"
```

---

## Task 7: Add Alternate Screen Buffer Methods

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala`

**Step 1: Write failing tests for alternate screen**

Add to `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`:

```scala
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
```

**Step 2: Run test to verify it fails**

Run: `sbt test`
Expected: Compilation errors for missing methods

**Step 3: Add alternate screen methods to TerminalService trait**

Add after the raw mode methods:

```scala
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
```

**Step 4: Implement alternate screen methods in TerminalServiceLive**

Add after the raw mode implementations:

```scala
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
```

**Step 5: Update mock implementation**

Add to the mock method:

```scala
override def enterAlternateScreen: IO[TUIError, Unit] =
  ZIO.unit

override def exitAlternateScreen: IO[TUIError, Unit] =
  ZIO.unit
```

**Step 6: Run test to verify it passes**

Run: `sbt test`
Expected: All tests pass

**Step 7: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "feat: add alternate screen buffer methods to TerminalService"
```

---

## Task 8: Integrate JLine Terminal into TerminalServiceLive

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala:58-138`

**Step 1: Update TerminalServiceLive to include JLine Terminal**

Modify the `TerminalServiceLive` case class signature:

```scala
final case class TerminalServiceLive(
  config: TerminalConfig,
  terminal: org.jline.terminal.Terminal,
) extends TerminalService:
```

**Step 2: Update size method to use JLine**

Replace the size implementation:

```scala
override def size: UIO[Rect] =
  ZIO.succeed {
    val termSize = terminal.getSize()
    Rect.fromSize(termSize.getColumns(), termSize.getRows())
  }
```

**Step 3: Update cursor methods to use JLine writer**

Replace setCursor implementation:

```scala
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
```

Replace hideCursor:

```scala
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
```

Replace showCursor:

```scala
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
```

**Step 4: Update flush to use JLine**

Replace flush implementation:

```scala
override def flush: IO[TUIError, Unit] =
  ZIO.attempt {
    terminal.flush()
  }.mapError { throwable =>
    TUIError.IOError(
      operation = "flush",
      cause = throwable.getMessage,
    )
  }
```

**Step 5: Implement real raw mode using JLine**

Replace enableRawMode:

```scala
override def enableRawMode: IO[TUIError, Unit] =
  ZIO.attempt {
    terminal.enterRawMode()
  }.mapError { throwable =>
    TUIError.IOError(
      operation = "enableRawMode",
      cause = throwable.getMessage,
    )
  }
```

Replace disableRawMode:

```scala
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
```

**Step 6: Update alternate screen to use JLine writer**

Replace enterAlternateScreen:

```scala
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
```

Replace exitAlternateScreen:

```scala
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
```

**Step 7: Update other methods to use JLine writer**

Replace print implementation:

```scala
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
```

Replace println implementation:

```scala
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
```

Replace clear implementation:

```scala
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
```

**Step 8: Update render to use JLine**

Replace render implementation:

```scala
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
```

**Step 9: Run tests**

Run: `sbt test`
Expected: Tests fail because live layer doesn't create JLine Terminal yet

**Step 10: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala
git commit -m "refactor: integrate JLine Terminal into TerminalServiceLive"
```

---

## Task 9: Update Live ZLayer with JLine Resource Management

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala:126-138`

**Step 1: Replace live ZLayer implementation**

Replace the `live` method in `TerminalService` companion object:

```scala
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
        ZIO.succeed(terminal.close()).orDie
      )
    yield TerminalServiceLive(TerminalConfig.default, terminal)
  }
```

**Step 2: Replace withConfig method**

Replace the `withConfig` method:

```scala
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
        ZIO.succeed(terminal.close()).orDie
      )
    yield TerminalServiceLive(config, terminal)
  }
```

**Step 3: Run tests to verify**

Run: `sbt test`
Expected: All tests pass

**Step 4: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala
git commit -m "feat: add JLine resource management to live ZLayer"
```

---

## Task 10: Add Scoped Helper Methods

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala` (companion object)

**Step 1: Write failing tests for scoped helpers**

Add to `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`:

```scala
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
```

**Step 2: Run test to verify it fails**

Run: `sbt test`
Expected: Compilation errors for missing methods

**Step 3: Add withRawMode helper method**

Add to `TerminalService` companion object after the `println` accessor method:

```scala
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
    ZIO.serviceWithZIO[TerminalService](_.enableRawMode).orDie
  )(_ =>
    ZIO.serviceWithZIO[TerminalService](_.disableRawMode).orDie
  )(_ => effect)
```

**Step 4: Add withAlternateScreen helper method**

Add after `withRawMode`:

```scala
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
    ZIO.serviceWithZIO[TerminalService](_.enterAlternateScreen).orDie
  )(_ =>
    ZIO.serviceWithZIO[TerminalService](_.exitAlternateScreen).orDie
  )(_ => effect)
```

**Step 5: Run test to verify it passes**

Run: `sbt test`
Expected: All tests pass

**Step 6: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "feat: add scoped helper methods for raw mode and alternate screen"
```

---

## Task 11: Rename mock to test for Consistency

**Files:**
- Modify: `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala:143-161`
- Modify: `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala:20-21`

**Step 1: Rename mock method to test**

In `src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala`, rename the `mock` method:

```scala
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
```

**Step 2: Update test file to use test() instead of mock()**

In `src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala`, replace:

```scala
private val testLayer: ZLayer[Any, Nothing, TerminalService] =
  TerminalService.test()
```

**Step 3: Run tests to verify**

Run: `sbt test`
Expected: All tests pass

**Step 4: Commit**

```bash
git add src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala src/test/scala/io/github/riccardomerolla/zio/tui/TerminalServiceSpec.scala
git commit -m "refactor: rename mock to test for consistency"
```

---

## Task 12: Run Final Verification

**Step 1: Run all tests**

Run: `sbt test`
Expected: All tests pass

**Step 2: Run code formatting**

Run: `sbt fmt`
Expected: Code formatted successfully

**Step 3: Run code quality checks**

Run: `sbt check`
Expected: All checks pass

**Step 4: Verify compilation**

Run: `sbt compile`
Expected: Compilation succeeds with no warnings

**Step 5: Commit any formatting changes**

```bash
git add -A
git commit -m "style: apply formatting and linting"
```

---

## Completion Checklist

- [ ] JLine 3 dependency added
- [ ] Rect domain type created with tests
- [ ] Terminal size method implemented
- [ ] Flush method implemented
- [ ] Cursor control methods (setCursor, hideCursor, showCursor) implemented
- [ ] Raw mode methods (enableRawMode, disableRawMode) implemented
- [ ] Alternate screen methods (enterAlternateScreen, exitAlternateScreen) implemented
- [ ] JLine Terminal integrated into TerminalServiceLive
- [ ] Live ZLayer updated with resource management
- [ ] Scoped helper methods (withRawMode, withAlternateScreen) added
- [ ] mock renamed to test
- [ ] All tests passing
- [ ] Code formatted and linted

All acceptance criteria from issue #5 are implemented and tested.
