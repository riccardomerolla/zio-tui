# zio-tui

A ZIO 2.x wrapper for [layoutz](https://github.com/mattlianje/layoutz), 
bringing effect-typed architecture to terminal UI development.

## Features

- 🎯 **Effect-typed**: All side effects properly typed with ZIO
- 🔧 **ZIO Ecosystem**: Native integration with ZIO HTTP, Streams, Config, Logging
- 📦 **Type-safe**: Resource management with ZLayer and Scope
- ⚡ **Concurrent**: Fiber-based concurrency model
- 🎨 **Beautiful**: Inherits all layoutz widgets and rendering

## Getting Started

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.riccardomerolla" %% "zio-tui" % "0.1.0"
```

## Quick Example

The simplest way to get started is with the **HelloTUIApp** example, which demonstrates the core `ZTuiApp` pattern:

```scala
import io.github.riccardomerolla.zio.tui._
import io.github.riccardomerolla.zio.tui.domain._
import zio._
import zio.stream._
import layoutz.Element

object HelloTUIApp extends ZIOAppDefault:
  
  // 1. Define your application state
  case class State(counter: Int, lastAction: String)
  
  // 2. Define messages (events that can happen)
  sealed trait Msg
  case object Increment extends Msg
  case object Decrement extends Msg
  case object Quit extends Msg
  
  // 3. Create your TUI app
  class HelloApp extends ZTuiApp[Any, Nothing, State, Msg]:
    
    // Initialize with starting state
    def init: ZIO[Any, Nothing, (State, ZCmd[Any, Nothing, Msg])] =
      ZIO.succeed((State(0, "Started"), ZCmd.none))
    
    // Update state in response to messages
    def update(msg: Msg, state: State): ZIO[Any, Nothing, (State, ZCmd[Any, Nothing, Msg])] =
      msg match
        case Increment => ZIO.succeed((state.copy(counter = state.counter + 1), ZCmd.none))
        case Decrement => ZIO.succeed((state.copy(counter = state.counter - 1), ZCmd.none))
        case Quit => ZIO.succeed((state, ZCmd.exit))
    
    // Subscribe to keyboard events
    def subscriptions(state: State): ZStream[Any, Nothing, Msg] =
      // Use ZSub.keyPress for real keyboard handling
      ZStream.empty
    
    // Render the current state
    def view(state: State): Element =
      layoutz.section(s"Counter: ${state.counter}")
  
  def run = new HelloApp().run()
```

See [HelloTUIApp.scala](src/main/scala/io/github/riccardomerolla/zio/tui/example/HelloTUIApp.scala) for a fully documented example with keyboard event handling.

Run it with:
```bash
sbt run
```

### Subscriptions

ZSub provides helpers for common subscription patterns:

```scala
import io.github.riccardomerolla.zio.tui.*
import zio.stream.*

def subscriptions(state: State): ZStream[Any, TUIError, Msg] =
  ZSub.merge(
    // Timer ticks
    ZSub.tick(1.second).map(_ => Msg.Tick),

    // File watching
    ZSub.watchFile("config.json")
      .map(content => Msg.ConfigChanged(content))
      .catchAll(err => ZStream.succeed(Msg.Error(err)))
  )
```

Available subscriptions:
- `ZSub.tick(interval)` - Emit at regular intervals
- `ZSub.watchFile(path)` - Monitor file changes
- `ZSub.merge(subs*)` - Combine multiple subscriptions

## Examples

### HelloTUIApp - Getting Started

The **[HelloTUIApp](src/main/scala/io/github/riccardomerolla/zio/tui/example/HelloTUIApp.scala)** is the simplest example demonstrating the core concepts of zio-tui:

**What it demonstrates:**
- ✅ **ZTuiApp trait**: The MVU (Model-View-Update) pattern for TUI applications
- ✅ **State management**: Immutable state with type-safe updates
- ✅ **Message handling**: Exhaustive pattern matching on events
- ✅ **Keyboard events**: Integration with terminal input (via subscriptions)
- ✅ **Effect-typed operations**: All side effects wrapped in ZIO
- ✅ **Resource safety**: Automatic cleanup and Scope management

**Architecture:**
1. **State** - Your application model (immutable data)
2. **Msg** - Events/actions that can happen (sealed ADT)
3. **init** - Initialize the starting state
4. **update** - Transform state in response to messages
5. **subscriptions** - Stream of external events (keyboard, timers, etc.)
6. **view** - Render state to UI elements

This pattern ensures:
- Type-safe state transitions
- Exhaustive event handling (compiler-checked)
- Clear separation of concerns
- Testable business logic

See the file for comprehensive documentation explaining each part in detail.

### Service Patterns

The library includes comprehensive examples demonstrating ZIO service patterns:

- **[DataSource](src/main/scala/io/github/riccardomerolla/zio/tui/example/DataSource.scala)** - Example service showing the complete service pattern with trait, Live/Test implementations, accessor methods, and layer constructors
- **[DataDashboardApp](src/main/scala/io/github/riccardomerolla/zio/tui/example/DataDashboardApp.scala)** - Multi-service composition example demonstrating horizontal (`++`) and vertical (`>>>`) layer composition
- **[SERVICE-PATTERNS.md](docs/SERVICE-PATTERNS.md)** - Comprehensive guide to ZIO service patterns with best practices, common mistakes, and real-world examples

### Counter Application

A minimal example demonstrating The Elm Architecture pattern:

- **[CounterApp](src/main/scala/io/github/riccardomerolla/zio/tui/example/CounterApp.scala)** - Counter demonstrating Model-View-Update pattern, state management, keyboard subscriptions, and pure view rendering in under 50 lines
- **[CounterAppSpec](src/test/scala/io/github/riccardomerolla/zio/tui/example/CounterAppSpec.scala)** - Comprehensive tests showing how to test ZTuiApp components

Run the interactive counter demo:

**Option 1: Standalone JAR**
```bash
sbt assembly
java --enable-native-access=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED \
  -jar target/scala-3.5.2/zio-tui-assembly-*.jar
```

**Option 2: Through sbt**
```bash
sbt "runMain io.github.riccardomerolla.zio.tui.example.CounterApp"
```

**Note on Keyboard Input:** The CounterApp requires a real terminal (TTY) for immediate keypress response. If you're running in an IDE console, piped input, or non-TTY environment, JLine3 will fall back to line-buffered input (requiring Enter after each key). For the best interactive experience, run the application in an actual terminal emulator (Terminal.app, iTerm2, Alacritty, etc.).

Study this example to understand how to structure interactive TUI applications with The Elm Architecture.

## Architecture

zio-tui follows effect-oriented programming principles:

- **Effects as blueprints**: All operations are ZIO effects - immutable descriptions of computations
- **Typed errors**: Errors are represented by domain-specific ADTs, not exceptions
- **Resource safety**: Terminal resources are managed with `ZLayer` and `Scope`
- **Composable services**: Use ZLayer for dependency injection

### Core Services

- `TerminalService`: Manages terminal initialization, rendering, and cleanup
- `WidgetRenderer`: Effect-typed widget rendering operations
- Domain models: `Widget`, `Layout`, `RenderResult`

## Development Workflow

- Keep all side effects inside `ZIO.*` constructors
- Model errors with sealed ADTs
- Inject dependencies with `ZLayer`
- Test with `zio-test`

## Contributing

Contributions are welcome! Please:
1. Respect the coding guidelines in `AGENTS.md`
2. Add or update tests for every change
3. Document noteworthy behavior in `README.md` and `CHANGELOG.md`
4. Run `sbt test` before opening a PR

## License

MIT License – see [LICENSE](LICENSE) for details.
