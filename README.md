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
