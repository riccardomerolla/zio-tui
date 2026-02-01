# TerminalService Enhancement Design

**Issue:** #5 - Enhance TerminalService with ZIO patterns
**Date:** 2026-02-01
**Status:** Approved

## Overview

Expand the existing `TerminalService` with comprehensive ZIO-based terminal operations including cursor control, raw mode management, alternate screen buffer, and terminal size detection.

## Design Decisions

### 1. Domain Types

Add a `Rect` type to represent terminal dimensions and positioned rectangles:

```scala
final case class Rect(
  x: Int,
  y: Int,
  width: Int,
  height: Int,
)

object Rect:
  def fromSize(width: Int, height: Int): Rect =
    Rect(x = 0, y = 0, width = width, height = height)
```

**Rationale:** Provides flexibility for future features beyond just terminal size, supports positioned rectangles.

### 2. Terminal Control Library

Use JLine 3 for terminal operations.

**Rationale:**
- Mature, widely-used library
- Cross-platform support (Unix, Windows, macOS)
- Comprehensive terminal control features
- Well-documented and battle-tested

**Dependency:**
```scala
libraryDependencies += "org.jline" % "jline" % "3.25.1"
```

### 3. Resource Management

Provide both manual methods and scoped helpers:

**Manual methods:** `enableRawMode`/`disableRawMode`, `enterAlternateScreen`/`exitAlternateScreen`
**Scoped helpers:** `withRawMode`, `withAlternateScreen`

**Rationale:** Manual methods match issue requirements and provide flexibility. Scoped helpers are idiomatic ZIO and prevent resource leaks through automatic cleanup.

### 4. Error Handling

Reuse existing `TUIError.IOError` for all new terminal operations.

**Rationale:** Simpler, consistent with current codebase. The `operation` field distinguishes between different failures.

### 5. Flush Implementation

Flush the output stream to ensure all buffered content is written.

**Rationale:** Standard terminal operation, ensures immediate visibility of output.

## API Design

### Enhanced TerminalService Trait

```scala
trait TerminalService:
  // Existing methods
  def render(widget: Widget): IO[TUIError, RenderResult]
  def renderAll(widgets: Seq[Widget]): IO[TUIError, Seq[RenderResult]]
  def clear: IO[TUIError, Unit]
  def print(text: String): IO[TUIError, Unit]
  def println(text: String): IO[TUIError, Unit]

  // New terminal operations
  def size: UIO[Rect]
  def flush: IO[TUIError, Unit]
  def setCursor(x: Int, y: Int): IO[TUIError, Unit]
  def hideCursor: IO[TUIError, Unit]
  def showCursor: IO[TUIError, Unit]
  def enableRawMode: IO[TUIError, Unit]
  def disableRawMode: IO[TUIError, Unit]
  def enterAlternateScreen: IO[TUIError, Unit]
  def exitAlternateScreen: IO[TUIError, Unit]
```

### Scoped Helper Methods

```scala
object TerminalService:
  def withRawMode[R, E, A](effect: ZIO[R & TerminalService, E, A]): ZIO[R & TerminalService, E, A]
  def withAlternateScreen[R, E, A](effect: ZIO[R & TerminalService, E, A]): ZIO[R & TerminalService, E, A]
```

## Implementation Strategy

### TerminalServiceLive

```scala
final case class TerminalServiceLive(
  config: TerminalConfig,
  terminal: org.jline.terminal.Terminal
) extends TerminalService
```

**Key implementations:**
- `size`: Uses `terminal.getSize()` to get current dimensions
- Cursor operations: Uses ANSI escape codes via `terminal.writer()`
- Raw mode: Uses `terminal.enterRawMode()` / `terminal.attributes()`
- Alternate screen: Uses ANSI escape sequences
- `flush`: Calls `terminal.flush()`

### Resource Management

```scala
object TerminalService:
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

**Benefits:**
- JLine Terminal is properly closed when layer is torn down
- Clean for-comprehension style
- Functional and effect-oriented
- Automatic resource cleanup via ZIO Scope

## Testing Strategy

### Test Implementation

```scala
def test(
  terminalSize: Rect = Rect.fromSize(80, 24),
  renderResults: Map[Widget, RenderResult] = Map.empty,
): ULayer[TerminalService]
```

All new methods return `ZIO.unit` or configured values without side effects.

### Unit Tests

- Test terminal size retrieval
- Test cursor operations
- Test scoped resource helpers (`withRawMode`, `withAlternateScreen`)
- Verify resource cleanup happens automatically

## Migration & Compatibility

### Backward Compatibility

- All existing methods unchanged
- Existing code continues to work
- Minor breaking change: `mock()` renamed to `test()` for consistency

### New Capabilities

```scala
// Dynamic terminal size
for
  rect <- terminal.size
  _ <- terminal.println(s"Terminal is ${rect.width}x${rect.height}")
yield ()

// Scoped resource management (recommended)
TerminalService.withRawMode {
  TerminalService.withAlternateScreen {
    myApp.run
  }
}

// Manual control (if needed)
for
  _ <- terminal.enterAlternateScreen
  _ <- terminal.enableRawMode
  _ <- myApp.run
  _ <- terminal.disableRawMode
  _ <- terminal.exitAlternateScreen
yield ()
```

## Implementation Order

1. Add JLine 3 dependency to `build.sbt`
2. Create `Rect` domain type
3. Enhance `TerminalService` trait with new method signatures
4. Implement `TerminalServiceLive` with JLine 3
5. Update `live` ZLayer with resource management
6. Add scoped helper methods (`withRawMode`, `withAlternateScreen`)
7. Update test implementation (rename `mock` → `test`)
8. Add unit tests for new operations
9. Update existing tests to use `test()` instead of `mock()`

## Acceptance Criteria

- [x] Define `TerminalService` trait with all terminal operations
- [x] Design `size`, `clear`, `flush` methods
- [x] Design cursor operations: `setCursor`, `hideCursor`, `showCursor`
- [x] Design raw mode management
- [x] Design alternate screen buffer operations
- [x] Plan ZLayer-based live implementation
- [x] Plan resource management with ZIO Scope
- [x] Plan unit tests with test backend

All acceptance criteria from issue #5 are addressed in this design.
