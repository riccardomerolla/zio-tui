# ZIO Service Patterns

Comprehensive guide to ZIO service patterns, when to use them, and best practices.

## Table of Contents

- [When to Use Services](#when-to-use-services)
- [Design Principles](#design-principles)
  - [1. Service Trait](#1-service-trait)
  - [2. Implementations](#2-implementations)
  - [3. Companion Object](#3-companion-object)
  - [4. Layer Composition](#4-layer-composition)
  - [5. Resource Management](#5-resource-management)
  - [6. Testing](#6-testing)
- [Real-World Examples](#real-world-examples)
- [Common Mistakes](#common-mistakes)

## When to Use Services

Use the ZIO service pattern when you have:

### Stateful Operations
Operations that maintain state between calls, such as:
- Database connections
- Cache systems
- External API clients with connection pools

### Shared Resources
Resources that should be created once and shared across your application:
- Connection pools
- File handles
- Thread pools

### Operations That Need Testing
Any operation where you want to:
- Test business logic without real I/O
- Provide deterministic test implementations
- Control behavior during testing

### Cross-Cutting Concerns
Common functionality used throughout your application:
- Logging
- Metrics collection
- Configuration management
- Error tracking

## Design Principles

### 1. Service Trait

The service trait defines the interface. Methods should return `ZIO[Any, E, A]` with dependencies provided through layers, not method parameters.

**Correct Pattern:**

```scala
trait DataSource:
  def stream: ZStream[Any, DataSourceError, DataPoint]
  def get(id: String): IO[DataSourceError, DataPoint]
  def save(point: DataPoint): IO[DataSourceError, Unit]
```

**Why This Matters:**
- Dependencies are in the layer, keeping methods clean
- Error types are explicit in the signature
- No implementation details leak into the interface
- Easy to compose with other ZIO effects

**Common Mistake:**

```scala
// DON'T: Dependencies in method signatures
trait DataSource:
  def get(id: String, config: Config): IO[DataSourceError, DataPoint]

// DON'T: Generic errors hide failure modes
trait DataSource:
  def get(id: String): Task[DataPoint]

// DON'T: Mixing sync and async operations
trait DataSource:
  def get(id: String): DataPoint  // blocking!
  def save(point: DataPoint): IO[DataSourceError, Unit]
```

**Real Example:**
See [`DataSource`](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataSource.scala) for a complete implementation.

### 2. Implementations

Provide both live and test implementations that share the same trait.

**Correct Pattern:**

```scala
// Live implementation with real dependencies
case class Live(storage: Ref[Map[String, DataPoint]]) extends DataSource:
  def stream: ZStream[Any, DataSourceError, DataPoint] =
    ZStream.fromZIO(storage.get).flatMap(map => ZStream.fromIterable(map.values))

  def get(id: String): IO[DataSourceError, DataPoint] =
    storage.get.flatMap { map =>
      ZIO.fromOption(map.get(id))
        .orElseFail(DataSourceError.NotFound(id))
    }

  def save(point: DataPoint): IO[DataSourceError, Unit] =
    storage.update(_ + (point.label -> point))

// Test implementation with predictable behavior
case class Test(data: Chunk[DataPoint]) extends DataSource:
  def stream: ZStream[Any, DataSourceError, DataPoint] =
    ZStream.fromIterable(data)

  def get(id: String): IO[DataSourceError, DataPoint] =
    ZIO.fromOption(data.find(_.label == id))
      .orElseFail(DataSourceError.NotFound(id))

  def save(point: DataPoint): IO[DataSourceError, Unit] =
    ZIO.unit  // Test implementation doesn't persist
```

**Why This Matters:**
- Test implementation provides deterministic behavior
- Both follow the same contract
- Production code and test code share the same interface
- Easy to swap implementations via layers

**Common Mistake:**

```scala
// DON'T: Test implementation that mutates shared state
case class Test(var data: List[DataPoint]) extends DataSource:
  def save(point: DataPoint): IO[DataSourceError, Unit] =
    data = data :+ point  // Mutable state in tests!
    ZIO.unit

// DON'T: Test implementation that does nothing useful
case class Test() extends DataSource:
  def get(id: String): IO[DataSourceError, DataPoint] =
    ZIO.succeed(DataPoint(0L, 0.0, ""))  // Always returns dummy data
```

**Real Examples:**
- [`DataSource.Live` and `DataSource.Test`](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataSource.scala)
- [`TerminalServiceLive` and test implementation](../src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala)
- [`HttpServiceLive` and test implementation](../src/main/scala/io/github/riccardomerolla/zio/tui/http/HttpService.scala)

### 3. Companion Object

The companion object provides accessor methods and layer constructors.

**Correct Pattern:**

```scala
object DataSource:
  // Accessor methods using ZIO.serviceWithZIO
  def stream: ZStream[DataSource, DataSourceError, DataPoint] =
    ZStream.serviceWithStream(_.stream)

  def get(id: String): ZIO[DataSource, DataSourceError, DataPoint] =
    ZIO.serviceWithZIO(_.get(id))

  def save(point: DataPoint): ZIO[DataSource, DataSourceError, Unit] =
    ZIO.serviceWithZIO(_.save(point))

  // Layer constructors with simple types
  val live: ZLayer[Any, Nothing, DataSource] =
    ZLayer.fromZIO(
      Ref.make(Map.empty[String, DataPoint]).map(Live(_))
    )

  def test(data: DataPoint*): ZLayer[Any, Nothing, DataSource] =
    ZLayer.succeed(Test(Chunk.fromIterable(data)))
```

**Why This Matters:**
- Accessor methods enable clean code: `DataSource.get("id")` instead of `ZIO.serviceWithZIO[DataSource](_.get("id"))`
- Layer types are simple and explicit
- Easy to discover available constructors
- Consistent pattern across all services

**Common Mistake:**

```scala
// DON'T: No accessor methods
object DataSource:
  val live: ZLayer[Any, Nothing, DataSource] = ???
  // Users forced to write: ZIO.serviceWithZIO[DataSource](_.get("id"))

// DON'T: Complex layer types that expose implementation
val live: ZLayer[Any, Nothing, Live] = ???  // Should be DataSource!

// DON'T: Layers that don't handle errors properly
val live: ZLayer[Any, Throwable, DataSource] = ???  // Forces error handling up
```

**Real Examples:**
- [`DataSource` companion object](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataSource.scala)
- [`TerminalService` companion object](../src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala)
- [`HttpService` companion object](../src/main/scala/io/github/riccardomerolla/zio/tui/http/HttpService.scala)

### 4. Layer Composition

Compose layers horizontally with `++` for independent services, and vertically with `>>>` for dependent services.

**Horizontal Composition (++): Independent Services**

Use when services don't depend on each other:

```scala
// Both services can be initialized independently
val appLayer: ZLayer[Any, Nothing, DataSource & TerminalService] =
  DataSource.live ++ TerminalService.live
```

**Vertical Composition (>>>): Dependent Services**

Use when one service depends on another:

```scala
// HttpService depends on ZIO HTTP Client
val live: ZLayer[Any, Throwable, HttpService] =
  Client.default >>> ZLayer.fromFunction(HttpServiceLive.apply)
```

**Mixed Composition**

Combine both patterns:

```scala
val layer =
  (DatabaseConfig.live >>> Database.live) ++
  HttpConfig.live ++
  TerminalService.live
```

**Why This Matters:**
- `++` creates parallel, independent initialization
- `>>>` chains dependencies in the correct order
- Type system ensures all dependencies are satisfied
- Clear expression of service relationships

**Common Mistake:**

```scala
// DON'T: Using >>> when services are independent (slower, unnecessary)
val appLayer = DataSource.live >>> TerminalService.live

// DON'T: Using ++ when there's a dependency (won't compile)
val httpLayer = HttpServiceLive.layer ++ Client.default  // Wrong order!

// DON'T: Creating complex dependency graphs manually
val manualLayer = ZLayer.fromZIO {
  for
    service1 <- ???
    service2 <- ???
    service3 <- ???
  yield ???
}
// Use >>> and ++ instead!
```

**Real Examples:**
- [`DataDashboardApp` horizontal composition](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataDashboardApp.scala)
- [`HttpService` vertical composition](../src/main/scala/io/github/riccardomerolla/zio/tui/http/HttpService.scala)

### 5. Resource Management

Use `ZLayer.scoped` for resources that need cleanup. ZIO handles resource acquisition and release automatically.

**Correct Pattern:**

```scala
val live: ZLayer[Any, Nothing, TerminalService] =
  ZLayer.scoped {
    for
      terminal <- ZIO.acquireRelease(
                    acquire = ZIO.attempt(
                      org.jline.terminal.TerminalBuilder.terminal()
                    ).orDie
                  )(release =
                    terminal =>
                      ZIO.succeed(terminal.close())
                  )
    yield TerminalServiceLive(TerminalConfig.default, terminal)
  }
```

**Why This Matters:**
- Resources are acquired when the layer is built
- Resources are released when the layer scope ends
- Cleanup happens even if the effect fails or is interrupted
- No resource leaks

**Common Mistake:**

```scala
// DON'T: Manual resource management
val live: ZLayer[Any, Nothing, TerminalService] =
  ZLayer.fromZIO {
    ZIO.attempt {
      val terminal = TerminalBuilder.terminal()
      TerminalServiceLive(TerminalConfig.default, terminal)
      // No cleanup! Terminal stays open forever
    }.orDie
  }

// DON'T: Cleanup in the service methods
trait TerminalService:
  def render(widget: Widget): IO[TUIError, RenderResult]
  def close(): UIO[Unit]  // Don't make users remember to call this!
```

**Real Examples:**
- [`TerminalService.live` with scoped resource](../src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala)

### 6. Testing

Provide test layers with predictable behavior. Test business logic separately from integration tests.

**Correct Pattern:**

```scala
// Test with predefined data
test("retrieves data point by label") {
  val point1 = DataPoint(timestamp = 1000L, value = 42.0, label = "cpu")
  val point2 = DataPoint(timestamp = 2000L, value = 84.0, label = "memory")
  val testLayer = DataSource.test(point1, point2)

  for result <- DataSource.get("cpu").provide(testLayer)
  yield assertTrue(result == point1)
}

// Test error cases
test("fails with NotFound for non-existent label") {
  val testLayer = DataSource.test()

  for result <- DataSource.get("nonexistent").provide(testLayer).exit
  yield assertTrue(
    result == Exit.fail(DataSourceError.NotFound("nonexistent"))
  )
}
```

**Why This Matters:**
- Tests are fast (no I/O)
- Tests are deterministic (same input = same output)
- Easy to test error conditions
- Business logic tested in isolation

**Test Helpers:**

Use test helpers for common patterns:

```scala
def testWith[R, E, A](
  layer: ZLayer[Any, Nothing, R]
)(
  test: ZIO[R, E, A]
): ZIO[Any, E, A] =
  test.provideLayer(layer)

def failingTest[R, E, A](
  layer: ZLayer[Any, Nothing, R]
)(
  test: ZIO[R, E, A]
)(
  assertion: E => Boolean
): ZIO[Any, Nothing, TestResult] =
  test.provideLayer(layer).either.map {
    case Left(error) if assertion(error) => assertTrue(true)
    case Left(error) => assertTrue(false) // Wrong error
    case Right(_) => assertTrue(false) // Should have failed
  }
```

**Common Mistake:**

```scala
// DON'T: Test implementation that throws exceptions
def test: ZLayer[Any, Nothing, DataSource] =
  ZLayer.succeed(new DataSource:
    def get(id: String): IO[DataSourceError, DataPoint] =
      throw new Exception("Not found")  // Should return ZIO.fail!
  )

// DON'T: Tests that depend on external services
test("gets data from database") {
  for result <- DataSource.get("id").provide(DataSource.live)
  yield assertTrue(result.id == "id")
}
// This is an integration test, not a unit test!

// DON'T: Sharing mutable state between tests
val sharedTestData = new AtomicReference(List.empty[DataPoint])
val testLayer = DataSource.test(sharedTestData)
// Tests will interfere with each other!
```

**Real Examples:**
- [`DataSourceSpec` test examples](../src/test/scala/io/github/riccardomerolla/zio/tui/example/DataSourceSpec.scala)
- [`TestHelpers` utilities](../src/test/scala/io/github/riccardomerolla/zio/tui/example/TestHelpers.scala)

## Real-World Examples

### Example 1: Simple Stateful Service

The [`DataSource`](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataSource.scala) demonstrates a stateful service with:
- Typed errors (`DataSourceError`)
- Multiple operations (`get`, `save`, `stream`)
- Live implementation with `Ref` for state
- Test implementation with fixed data
- Clean accessor methods

Key takeaways:
- State is encapsulated in the implementation
- Interface is pure and effect-typed
- Easy to test without real I/O

### Example 2: Service with Resource Management

The [`TerminalService`](../src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala) demonstrates resource management with:
- `ZLayer.scoped` for terminal lifecycle
- `ZIO.acquireRelease` for proper cleanup
- Helper methods (`withRawMode`, `withAlternateScreen`) for scoped operations
- Test implementation that does nothing

Key takeaways:
- Resources acquired once and shared
- Automatic cleanup on shutdown or failure
- Scoped helpers for temporary mode changes

### Example 3: Service with Dependencies

The [`HttpService`](../src/main/scala/io/github/riccardomerolla/zio/tui/http/HttpService.scala) demonstrates vertical composition with:
- Dependency on ZIO HTTP `Client`
- Vertical composition: `Client.default >>> ZLayer.fromFunction(...)`
- Test implementation returning predefined responses

Key takeaways:
- Dependencies declared in layer type
- Composed vertically with `>>>`
- Test layer provides mocked responses

### Example 4: Multi-Service Application

The [`DataDashboardApp`](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataDashboardApp.scala) demonstrates composition with:
- Multiple services (`DataSource & TerminalService`)
- Horizontal composition with `++`
- Clean program using accessor methods
- Type-safe error handling

Key takeaways:
- Services composed independently with `++`
- Accessor methods keep code clean
- Errors unified with union types

## Common Mistakes

### Mistake 1: Dependencies in Method Signatures

**Wrong:**
```scala
trait DataSource:
  def get(id: String, config: Config): IO[DataSourceError, DataPoint]
```

**Right:**
```scala
trait DataSource:
  def get(id: String): IO[DataSourceError, DataPoint]

case class Live(config: Config) extends DataSource:
  def get(id: String): IO[DataSourceError, DataPoint] = ???
```

### Mistake 2: Exposing Implementation in Layer Type

**Wrong:**
```scala
val live: ZLayer[Any, Nothing, TerminalServiceLive] = ???
```

**Right:**
```scala
val live: ZLayer[Any, Nothing, TerminalService] = ???
```

### Mistake 3: Not Using Accessor Methods

**Wrong:**
```scala
val program = ZIO.serviceWithZIO[DataSource](_.get("id"))
```

**Right:**
```scala
object DataSource:
  def get(id: String): ZIO[DataSource, DataSourceError, DataPoint] =
    ZIO.serviceWithZIO(_.get(id))

val program = DataSource.get("id")
```

### Mistake 4: Generic Errors Instead of Typed Errors

**Wrong:**
```scala
trait DataSource:
  def get(id: String): Task[DataPoint]  // Throwable - too generic!
```

**Right:**
```scala
enum DataSourceError extends Exception:
  case NotFound(id: String)
  case ConnectionFailed(reason: String)

trait DataSource:
  def get(id: String): IO[DataSourceError, DataPoint]
```

### Mistake 5: Manual Resource Management

**Wrong:**
```scala
val live: ZLayer[Any, Nothing, TerminalService] =
  ZLayer.fromZIO {
    ZIO.attempt {
      val terminal = TerminalBuilder.terminal()
      TerminalServiceLive(terminal)
    }.orDie
  }
  // Terminal never closed!
```

**Right:**
```scala
val live: ZLayer[Any, Nothing, TerminalService] =
  ZLayer.scoped {
    for
      terminal <- ZIO.acquireRelease(
                    acquire = ZIO.attempt(TerminalBuilder.terminal()).orDie
                  )(release = terminal => ZIO.succeed(terminal.close()))
    yield TerminalServiceLive(terminal)
  }
```

### Mistake 6: Wrong Composition Operator

**Wrong:**
```scala
// Services are independent, but using vertical composition
val layer = DataSource.live >>> TerminalService.live
```

**Right:**
```scala
// Use horizontal composition for independent services
val layer = DataSource.live ++ TerminalService.live
```

### Mistake 7: Test Implementation with Side Effects

**Wrong:**
```scala
case class Test() extends DataSource:
  def get(id: String): IO[DataSourceError, DataPoint] =
    ZIO.attempt {
      // Reading from actual database in tests!
      database.query(s"SELECT * FROM data WHERE id = '$id'")
    }.mapError(_ => DataSourceError.NotFound(id))
```

**Right:**
```scala
case class Test(data: Chunk[DataPoint]) extends DataSource:
  def get(id: String): IO[DataSourceError, DataPoint] =
    ZIO.fromOption(data.find(_.label == id))
      .orElseFail(DataSourceError.NotFound(id))
```

## Summary

Follow these principles for clean, testable, composable ZIO services:

1. **Service traits**: Return `ZIO[Any, E, A]`, with typed errors
2. **Implementations**: Provide both live and test implementations
3. **Companion objects**: Include accessor methods and simple layer constructors
4. **Layer composition**: Use `++` for independent services, `>>>` for dependencies
5. **Resource management**: Use `ZLayer.scoped` and `ZIO.acquireRelease`
6. **Testing**: Provide deterministic test layers without I/O

By following these patterns, you'll create services that are:
- Easy to test
- Easy to compose
- Resource-safe
- Type-safe
- Maintainable

For complete working examples, see:
- [`DataSource.scala`](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataSource.scala)
- [`TerminalService.scala`](../src/main/scala/io/github/riccardomerolla/zio/tui/service/TerminalService.scala)
- [`HttpService.scala`](../src/main/scala/io/github/riccardomerolla/zio/tui/http/HttpService.scala)
- [`DataDashboardApp.scala`](../src/main/scala/io/github/riccardomerolla/zio/tui/example/DataDashboardApp.scala)
