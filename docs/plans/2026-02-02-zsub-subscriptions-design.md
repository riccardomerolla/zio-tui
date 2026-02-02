# ZSub Subscriptions Module Design

**Issue**: #7
**Date**: 2026-02-02
**Status**: Approved

## Overview

Create a subscription helpers module using ZIO Streams for common TUI patterns. The `ZSub` object provides factory methods for timer ticks, file monitoring, keyboard events, and subscription composition.

## Goals

- Provide ergonomic subscription helpers for common TUI patterns
- Integrate seamlessly with existing `ZTuiApp` architecture
- Use typed errors following existing error handling patterns
- Support backpressure and proper resource management
- Enable composable, testable subscription streams

## Module Structure

```
src/main/scala/io/github/riccardomerolla/zio/tui/subscriptions/
├── ZSub.scala              # Main API object with factory methods
├── Key.scala               # Key event ADT for keyboard input
└── SubscriptionError.scala # Typed errors for subscription failures
```

## Core Types

### Key Event Model

```scala
sealed trait Key
object Key:
  case class Character(char: Char) extends Key
  case class Special(name: String) extends Key  // Arrow keys, F-keys, etc.
  case class Control(char: Char) extends Key    // Ctrl+C, Ctrl+D, etc.
  case object Enter extends Key
  case object Escape extends Key
  case object Backspace extends Key
  case object Tab extends Key
```

**Design Rationale**: Custom ADT provides type safety and pattern matching ergonomics, avoiding raw strings/ints from jline.

### Error Types

```scala
sealed trait SubscriptionError extends TUIError
object SubscriptionError:
  case class FileNotFound(path: String) extends SubscriptionError
  case class IOError(message: String, cause: Throwable) extends SubscriptionError
  case class TerminalReadError(cause: Throwable) extends SubscriptionError
```

**Design Rationale**: Fallible streams allow app-level error handling. Extends `TUIError` for type alignment with `ZTuiApp`.

## ZSub API

### Method Signatures

```scala
object ZSub:
  def tick(interval: Duration): ZStream[Any, Nothing, Unit]

  def watchFile(path: String): ZStream[Any, SubscriptionError, String]

  def keyPress[Msg](handler: Key => Option[Msg]): ZStream[Any, SubscriptionError, Msg]

  def merge[R, E, Msg](subs: ZStream[R, E, Msg]*): ZStream[R, E, Msg]
```

### Implementation Details

#### tick
- **Purpose**: Emit Unit at regular intervals for periodic tasks
- **Implementation**: `ZStream.repeatWithSchedule(ZIO.unit, Schedule.fixed(interval))`
- **Error Handling**: Infallible (Nothing error type)
- **Backpressure**: Natural rate limiting via fixed schedule

#### watchFile
- **Purpose**: Monitor file changes and emit content
- **Implementation**:
  - Poll file periodically with `Schedule.fixed(100.millis)`
  - Read content and compare MD5 hash
  - Emit only when content differs
  - Simple polling approach for cross-platform compatibility
- **Error Handling**: Fallible with `FileNotFound`, `IOError`
- **Backpressure**: Polling rate naturally bounds production

#### keyPress
- **Purpose**: Capture keyboard input and transform to messages
- **Implementation**:
  - Read from jline's `Terminal.reader()` in ZStream
  - Parse raw input into Key ADT events
  - Apply handler function to filter/transform into messages
  - Handler returns `Option[Msg]` for selective message emission
- **Error Handling**: Fallible with `TerminalReadError`
- **Backpressure**: Terminal input naturally bounded by user typing speed

#### merge
- **Purpose**: Combine multiple subscriptions into single stream
- **Implementation**: Wrapper around `ZStream.mergeAll(subs: _*)`
- **Error Handling**: Preserves error type from input streams
- **Backpressure**: ZStream's built-in backpressure across all merged streams (16-element default buffer)

## Integration with ZTuiApp

### Usage Pattern

```scala
class MyTuiApp extends ZTuiApp[Any, TUIError, State, Msg]:

  def subscriptions(state: State): ZStream[Any, TUIError, Msg] =
    ZSub.merge(
      // Timer tick every second
      ZSub.tick(1.second).map(_ => Msg.Tick),

      // Keyboard input
      ZSub.keyPress {
        case Key.Character('q') => Some(Msg.Quit)
        case Key.Character('r') => Some(Msg.Refresh)
        case Key.Control('c')   => Some(Msg.Quit)
        case _                  => None
      },

      // File watching with error handling
      ZSub.watchFile("config.json")
        .map(content => Msg.ConfigChanged(content))
        .catchAll(err => ZStream.succeed(Msg.Error(err)))
    )
```

### Design Points

1. **Type Alignment**: `ZStream[R, E, Msg]` composes naturally with `ZTuiApp.subscriptions`
2. **Error Handling**: `SubscriptionError extends TUIError` enables direct usage or custom handling
3. **Composability**: `merge` provides ergonomic multi-source subscription
4. **Message Transformation**: `.map()` converts events to app-specific messages (MVU pattern)
5. **Conditional Subscriptions**: Use state parameter for conditional subscription activation

## Testing Strategy

### Unit Tests (zio-test)

1. **ZSubSpec.scala**:
```scala
test("tick emits at regular intervals") {
  for
    _ <- TestClock.adjust(5.seconds)
    chunks <- ZSub.tick(1.second).take(5).runCollect
  yield assertTrue(chunks.length == 5)
}

test("watchFile emits only on content changes") {
  // Use TestSystem to mock file changes
  // Verify emissions only when content differs
}

test("keyPress filters and transforms correctly") {
  // Mock keyboard input stream
  // Verify handler function applied correctly
}

test("merge combines multiple streams") {
  // Verify all sources emit through merged stream
  // Verify backpressure handling
}
```

2. **KeySpec.scala**: Test Key ADT parsing and pattern matching
3. **SubscriptionErrorSpec.scala**: Test error type hierarchy

### Examples

1. **examples/TickerApp.scala**: Simple clock app using `tick`
2. **examples/FileWatcherApp.scala**: Config file monitor using `watchFile`
3. **examples/InteractiveApp.scala**: Full TUI with keyboard navigation using `keyPress` and `merge`

## Documentation Requirements

Each subscription method needs:
- Scaladoc with usage examples
- Error scenarios documented
- Backpressure behavior noted
- Integration examples with ZTuiApp
- Performance characteristics (polling intervals, etc.)

## Acceptance Criteria

- [x] Design approved for `zio.tui.subscriptions` package
- [ ] Implement `ZSub.tick` for timer ticks
- [ ] Implement `ZSub.watchFile` for file monitoring
- [ ] Implement `ZSub.keyPress` for keyboard events
- [ ] Implement `ZSub.merge` for combining subscriptions
- [ ] Add backpressure handling (via ZStream built-ins)
- [ ] Add examples for each subscription type
- [ ] Add unit tests for all subscription types
- [ ] Update package.scala exports

## Non-Goals

- OS-native file watching (using simple polling instead)
- Complex keyboard input (mouse events, multi-key chords)
- Subscription lifecycle management beyond ZStream semantics
- Custom backpressure strategies (using ZStream defaults)

## Future Enhancements

- `ZSub.watchDirectory` for directory monitoring
- `ZSub.interval` with dynamic interval adjustment
- `ZSub.mouse` for mouse event handling
- Custom backpressure strategies via configuration
