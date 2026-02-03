package io.github.riccardomerolla.zio.tui.example

import zio.*

import io.github.riccardomerolla.zio.tui.*

/** Example application demonstrating multi-service composition.
  *
  * This application showcases:
  *   - Combining multiple services in a single program
  *   - Horizontal composition with `++` for independent services
  *   - Type-level service composition (`DataSource & TerminalService`)
  *   - Clean program structure using accessor methods
  */
object DataDashboardApp extends ZIOAppDefault:

  /** Application that uses both DataSource and TerminalService.
    *
    * This demonstrates:
    *   - Using accessor methods for clean code (DataSource.stream, TerminalService.render, etc.)
    *   - Composing effects that require multiple services
    *   - Type-safe error handling with typed errors
    */
  val program: ZIO[DataSource & TerminalService, DataSourceError | TUIError, Unit] =
    for
      // Use accessor methods for clean code
      points <- DataSource.stream.runCollect
      widget <- ZIO.succeed(
                  Widget.section("Data Dashboard")(
                    points.map(p => s"${p.label}: ${p.value}").mkString("\n")
                  )
                )
      result <- TerminalService.render(widget)
      _      <- TerminalService.println(result.output)
    yield ()

  /** Compose layers horizontally with ++.
    *
    * Horizontal composition combines independent services that don't depend on each other. Both DataSource and
    * TerminalService can be initialized independently, so we use `++` to combine them.
    */
  val appLayer: ZLayer[Any, Nothing, DataSource & TerminalService] =
    DataSource.live ++ TerminalService.live

  def run: ZIO[Environment & (ZIOAppArgs & Scope), Any, Any] =
    program.provide(appLayer)

// ============================================================================
// Layer Composition Patterns
// ============================================================================
//
// ZLayer provides two primary composition operators:
//
// 1. HORIZONTAL COMPOSITION (++): Combine independent services
//    Use when services don't depend on each other
//
//    Example:
//    ```scala
//    val layer = ServiceA.live ++ ServiceB.live ++ ServiceC.live
//    ```
//
//    This creates a layer that provides ServiceA, ServiceB, and ServiceC.
//    All services are initialized independently and in parallel.
//
// 2. VERTICAL COMPOSITION (>>>): Chain dependent services
//    Use when one service depends on another
//
//    Example:
//    ```scala
//    // Service B depends on Service A
//    trait ServiceB:
//      def processData(input: String): Task[String]
//
//    object ServiceB:
//      case class Live(serviceA: ServiceA) extends ServiceB:
//        def processData(input: String): Task[String] =
//          serviceA.transform(input).map(_.toUpperCase)
//
//      val live: ZLayer[ServiceA, Nothing, ServiceB] =
//        ZLayer.fromFunction(Live(_))
//
//    // Compose vertically with >>>
//    val composed: ZLayer[Any, Nothing, ServiceB] =
//      ServiceA.live >>> ServiceB.live
//    ```
//
//    The output of the left layer becomes the input to the right layer.
//    ServiceA is initialized first, then passed to ServiceB.
//
// 3. MIXED COMPOSITION: Combine both patterns
//
//    Example:
//    ```scala
//    val layer =
//      (DatabaseConfig.live >>> Database.live) ++
//      HttpConfig.live ++
//      TerminalService.live
//    ```
//
//    This creates a layer that:
//    - Chains DatabaseConfig into Database (vertical)
//    - Combines the result with independent HttpConfig and TerminalService (horizontal)
//
// 4. LAYER DEPENDENCY TYPES:
//
//    - ZLayer[Any, E, A]: Layer has no dependencies
//    - ZLayer[R, E, A]: Layer requires R to be provided
//    - ZLayer[R1 & R2, E, A]: Layer requires both R1 and R2
//
//    When composing:
//    - R1 ++ R2 produces R1 & R2
//    - R1 >>> R2 (where R2 needs R1) produces R2
//
// Choose the right composition:
// - Use ++ when services are independent (like DataSource and TerminalService)
// - Use >>> when services depend on each other (like Config -> Database -> Repository)
// - Mix them when you have both independent and dependent services
