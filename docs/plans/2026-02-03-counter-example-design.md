# Counter Example Application Design

**Date**: 2026-02-03
**Issue**: #10
**Goal**: Create a counter app demonstrating The Elm Architecture pattern with keyboard subscriptions

## Overview

Create a simple counter application that demonstrates the core ZIO-TUI patterns:
- The Elm Architecture (Model-View-Update)
- Keyboard input subscriptions
- State management
- Effect composition

## Components

### 1. KeyPress Subscription Helper

**Location**: `src/main/scala/io/github/riccardomerolla/zio/tui/subscriptions/ZSub.scala`

Add `keyPress` method to enable keyboard input handling:

```scala
def keyPress[Msg](handler: Key => Option[Msg]): ZStream[Any, Nothing, Msg]
```

**Implementation approach**:
- Use JLine3's `NonBlockingReader` (already a dependency)
- Read characters asynchronously in a ZIO stream
- Convert raw input to `Key` ADT
- Apply user handler to produce messages
- Filter out `None` results

### 2. Counter Application

**Location**: `src/main/scala/io/github/riccardomerolla/zio/tui/example/CounterApp.scala`

**State**:
```scala
case class CounterState(count: Int)
```

**Messages**:
```scala
enum CounterMsg:
  case Increment
  case Decrement
  case Reset
  case Quit
```

**Application Structure**:
- Implements `ZTuiApp[Any, Nothing, CounterState, CounterMsg]`
- `init`: Returns `(CounterState(0), ZCmd.none)`
- `update`: Pattern matches on messages to transform state
- `subscriptions`: Maps keyboard input (+, -, r, q) to messages
- `view`: Renders current count and instructions using layoutz Elements

**UI Output**:
```
=== ZIO Counter ===
Count: 5

Press '+' to increment
Press '-' to decrement
Press 'r' to reset
Press 'q' to quit
```

### 3. Test

**Location**: `src/test/scala/io/github/riccardomerolla/zio/tui/example/CounterAppSpec.scala`

Test cases:
- Initial state is 0
- Increment increases count
- Decrement decreases count
- Reset returns to 0
- Quit produces exit command
- View renders correct count

### 4. Documentation

Update examples README section in main README.md to include Counter example.

## Constraints

- Keep CounterApp < 50 lines of code
- No external dependencies beyond existing ones (ZIO, layoutz, JLine)
- Follow existing code style and patterns
- Include comprehensive documentation

## Success Criteria

- [x] CounterState case class
- [x] CounterMsg enum
- [x] Uses ZTuiApp trait
- [x] Keyboard subscriptions work (+, -, r, q keys)
- [x] Clear documentation
- [x] Under 50 lines of code
- [x] Tests pass
- [x] Added to README
