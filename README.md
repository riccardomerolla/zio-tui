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

```scala
import io.github.riccardomerolla.zio.tui._
import zio._

object HelloTUI extends ZIOAppDefault:
  def run =
    for
      terminal <- ZIO.service[TerminalService]
      widget   <- Widget.text("Hello, ZIO TUI!")
      _        <- terminal.render(widget)
    yield ()
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

**Option 1: Standalone JAR (recommended for full terminal support)**
```bash
sbt assembly
java --enable-native-access=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED \
  -jar target/scala-3.5.2/zio-tui-assembly-*.jar
```

**Option 2: Through sbt (limited terminal support)**
```bash
sbt "runMain io.github.riccardomerolla.zio.tui.example.CounterApp"
```

Note: For immediate keypress response without requiring Enter, use the standalone JAR option. Running through sbt has limited terminal control capabilities.

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
