# ZLayer Service Patterns Implementation Design

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create comprehensive documentation and examples for ZLayer service patterns in zio-tui.

**Architecture:** Build on existing TerminalService and HttpService patterns to create DataSource example service, multi-service composition examples, testing utilities, and best practices guide.

**Tech Stack:** ZIO 2.x, ZLayer, zio-test

---

## Overview

This design establishes standardized patterns for creating and composing ZIO services in the zio-tui library. We'll create a complete example service (DataSource) that demonstrates the full service pattern, show how to compose multiple services together, and document best practices.

## 1. DataSource Example Service

### Purpose
Demonstrate the complete service pattern with a realistic example that shows:
- Service trait definition with typed errors
- Live implementation with resource management
- Test implementation for testing
- Accessor methods for ergonomic usage
- Layer construction

### Service Structure

**File:** `src/main/scala/io/github/riccardomerolla/zio/tui/examples/DataSource.scala`

```scala
package io.github.riccardomerolla.zio.tui.examples

import zio.*
import zio.stream.*

// Domain model
case class DataPoint(timestamp: Long, value: Double, label: String)

// Typed errors
enum DataSourceError extends Exception:
  case ConnectionFailed(reason: String) extends DataSourceError
  case InvalidData(message: String) extends DataSourceError
  case NotFound(id: String) extends DataSourceError

// Service trait
trait DataSource:
  def stream: ZStream[Any, DataSourceError, DataPoint]
  def get(id: String): IO[DataSourceError, DataPoint]
  def save(point: DataPoint): IO[DataSourceError, Unit]

// Companion object with layers and accessors
object DataSource:
  // Accessor methods
  def stream: ZStream[DataSource, DataSourceError, DataPoint] =
    ZStream.serviceWithStream(_.stream)

  def get(id: String): ZIO[DataSource, DataSourceError, DataPoint] =
    ZIO.serviceWithZIO(_.get(id))

  def save(point: DataPoint): ZIO[DataSource, DataSourceError, Unit] =
    ZIO.serviceWithZIO(_.save(point))

  // Live implementation
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

  // Test implementation
  case class Test(data: Chunk[DataPoint]) extends DataSource:
    def stream: ZStream[Any, DataSourceError, DataPoint] =
      ZStream.fromIterable(data)

    def get(id: String): IO[DataSourceError, DataPoint] =
      ZIO.fromOption(data.find(_.label == id))
        .orElseFail(DataSourceError.NotFound(id))

    def save(point: DataPoint): IO[DataSourceError, Unit] =
      ZIO.unit

  // Layer constructors
  val live: ZLayer[Any, Nothing, DataSource] =
    ZLayer.fromZIO(
      Ref.make(Map.empty[String, DataPoint]).map(Live(_))
    )

  def test(data: DataPoint*): ZLayer[Any, Nothing, DataSource] =
    ZLayer.succeed(Test(Chunk.fromIterable(data)))
```

### Design Rationale

**Trait Design:**
- Methods return `ZIO[Any, E, A]` or `ZStream[Any, E, A]` (no R in service methods)
- Service dependencies go in layer construction, not method signatures
- Typed errors in the error channel for type-safe error handling

**Accessor Pattern:**
- Static methods in companion object for ergonomic usage
- `ZIO.serviceWithZIO` for effect methods
- `ZStream.serviceWithStream` for streaming methods
- Enables usage: `DataSource.get("foo")` instead of `ZIO.service[DataSource].flatMap(_.get("foo"))`

**Implementation Separation:**
- Live implementation as case class with dependencies
- Test implementation with minimal dependencies
- Both implement the same trait

**Layer Construction:**
- `live` layer handles resource allocation
- `test` layer provides controllable test data
- Layers are `ZLayer[Any, Nothing, DataSource]` when possible

## 2. Multi-Service Composition

### Purpose
Show how to combine multiple services in real applications using both horizontal composition (`++`) and vertical composition (`>>>`).

### Data Dashboard Example

**File:** `src/main/scala/io/github/riccardomerolla/zio/tui/examples/DataDashboardApp.scala`

```scala
package io.github.riccardomerolla.zio.tui.examples

import io.github.riccardomerolla.zio.tui.*
import zio.*
import zio.stream.*
import layoutz.*

object DataDashboardApp extends ZIOAppDefault:

  // Application that uses both DataSource and TerminalService
  val program: ZIO[DataSource & TerminalService, Any, Unit] =
    for
      // Use accessor methods for clean code
      points   <- DataSource.stream.runCollect
      widget   <- ZIO.succeed(
                    Widget.box(
                      points.map(p => s"${p.label}: ${p.value}").mkString("\n")
                    )
                  )
      result   <- TerminalService.render(widget)
      _        <- TerminalService.println(result.output)
    yield ()

  // Compose layers horizontally with ++
  val appLayer: ZLayer[Any, Nothing, DataSource & TerminalService] =
    DataSource.live ++ TerminalService.live

  def run = program.provide(appLayer)
```

### Vertical Composition Example

For services that depend on each other:

```scala
// Service B depends on Service A
trait ServiceB:
  def processData(input: String): Task[String]

object ServiceB:
  case class Live(serviceA: ServiceA) extends ServiceB:
    def processData(input: String): Task[String] =
      serviceA.transform(input).map(_.toUpperCase)

  val live: ZLayer[ServiceA, Nothing, ServiceB] =
    ZLayer.fromFunction(Live(_))

// Compose vertically with >>>
val composed: ZLayer[Any, Nothing, ServiceB] =
  ServiceA.live >>> ServiceB.live
```

### Composition Patterns

**Horizontal (`++`):** Combine independent services
```scala
val layer = ServiceA.live ++ ServiceB.live ++ ServiceC.live
```

**Vertical (`>>>`):** Chain dependent services
```scala
val layer = DatabaseConfig.live >>> Database.live >>> Repository.live
```

**Mixed:**
```scala
val layer =
  (DatabaseConfig.live >>> Database.live) ++
  HttpConfig.live ++
  TerminalService.live
```

## 3. Testing Utilities

### Purpose
Provide reusable utilities for testing code that depends on ZIO services.

### TestHelpers Module

**File:** `src/test/scala/io/github/riccardomerolla/zio/tui/examples/TestHelpers.scala`

```scala
package io.github.riccardomerolla.zio.tui.examples

import zio.*
import zio.test.*

object TestHelpers:

  /** Run a test with specific service implementations */
  def testWith[R, E, A](
    layer: ZLayer[Any, Nothing, R]
  )(
    test: ZIO[R, E, A]
  ): ZIO[Any, E, A] =
    test.provideLayer(layer)

  /** Create a test that expects a specific error */
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

### Usage in Tests

```scala
object DataSourceSpec extends ZIOSpecDefault:

  val testData = Chunk(
    DataPoint(1000, 42.0, "temperature"),
    DataPoint(2000, 75.0, "humidity")
  )

  def spec = suite("DataSource")(
    test("retrieves existing data point") {
      val program = DataSource.get("temperature")

      testWith(DataSource.test(testData*))(program).map { point =>
        assertTrue(point.value == 42.0)
      }
    },

    test("fails when data point not found") {
      val program = DataSource.get("pressure")

      failingTest(DataSource.test(testData*))(program) {
        case DataSourceError.NotFound(id) => id == "pressure"
        case _ => false
      }
    }
  )
```

## 4. Best Practices Documentation

### File Structure
**File:** `docs/SERVICE-PATTERNS.md`

### Content Outline

**When to Use Services:**
- Stateful operations (database, cache, external APIs)
- Shared resources (connection pools, file handles)
- Operations that need testing with different implementations
- Cross-cutting concerns (logging, metrics, configuration)

**Design Principles:**

1. **Service Trait:**
   - Methods return `ZIO[Any, E, A]` (dependencies in layer, not methods)
   - Typed errors in error channel
   - Pure interface, no implementation details

2. **Implementations:**
   - Live implementation as case class with dependencies
   - Test implementation with controllable behavior
   - Both implement the same trait

3. **Companion Object:**
   - Accessor methods using `ZIO.serviceWithZIO`
   - Layer constructors: `live`, `test`, custom variants
   - Keep layer types simple: `ZLayer[Deps, Err, Service]`

4. **Layer Composition:**
   - Horizontal (`++`): Independent services
   - Vertical (`>>>`): Dependent services
   - Prefer composition over monolithic services

5. **Resource Management:**
   - Use `ZLayer.scoped` for resources that need cleanup
   - Acquire resources in layer construction
   - Release handled automatically by ZIO

6. **Testing:**
   - Provide test layers with predictable behavior
   - Use accessor methods in tests for clean code
   - Test business logic separately from integration

### Code Examples in Documentation

Each principle includes:
- Correct example showing the pattern
- Common mistake to avoid
- Explanation of why it matters

## Implementation Checklist

1. Create DataSource service with domain model, error types, trait, and implementations
2. Add accessor methods and layer constructors to companion object
3. Create DataDashboardApp showing multi-service composition
4. Implement TestHelpers utilities for testing
5. Write comprehensive tests for DataSource service
6. Create SERVICE-PATTERNS.md documentation
7. Add examples section to main README
8. Update package exports if needed
9. Run tests and verify coverage
10. Commit implementation

## Success Criteria

- DataSource service follows established patterns from TerminalService and HttpService
- Multi-service composition example compiles and demonstrates both `++` and `>>>`
- Testing utilities work with different service implementations
- Documentation clearly explains when and how to use each pattern
- All tests pass with good coverage
- Code follows existing style (scalafmt, scalafix)
