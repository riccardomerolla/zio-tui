package io.github.riccardomerolla.zio.tui.example

import zio.*

import io.github.riccardomerolla.zio.tui.*

/** Example application demonstrating HTTP integration with zio-tui.
  *
  * This application showcases:
  *   - Simple HTTP requests with ZHttp
  *   - HttpService with dependency injection
  *   - Periodic polling with ZIO Schedule
  *   - Integration of HTTP responses with TUI widgets
  *   - Error handling with typed HttpError
  */
object HttpIntegrationApp extends ZIOAppDefault:

  /** Demonstrate simple HTTP GET with ZHttp. */
  def simpleGetDemo: ZIO[TerminalService & zio.http.Client, HttpError | TUIError, Unit] =
    for
      _        <- TerminalService.println("\n=== Simple GET Request Demo ===")
      // Make a simple GET request
      response <- ZIO.scoped(ZHttp.get("https://httpbin.org/get"))
      _        <- TerminalService.println(s"Status: ${response.status}")
      _        <- TerminalService.println(s"Content-Type: ${response.contentType.getOrElse("unknown")}")
      // Display response in a widget
      widget   <- ZIO.succeed(
                    Widget.section("HTTP Response")(
                      s"Status: ${response.status}\nBody length: ${response.body.length} bytes"
                    )
                  )
      result   <- TerminalService.render(widget)
      _        <- TerminalService.println(result.output)
    yield ()

  /** Demonstrate POST request with body. */
  def postDemo: ZIO[TerminalService & zio.http.Client, HttpError | TUIError, Unit] =
    for
      _        <- TerminalService.println("\n=== POST Request Demo ===")
      // Make a POST request with JSON body
      response <- ZIO.scoped(ZHttp.post(
                    url = "https://httpbin.org/post",
                    body = """{"message": "Hello from zio-tui!"}""",
                    headers = Map("Content-Type" -> "application/json"),
                  ))
      _        <- TerminalService.println(s"Status: ${response.status}")
      _        <- TerminalService.println(s"Response received: ${response.body.take(100)}...")
    yield ()

  /** Demonstrate custom request with HttpRequest builder. */
  def requestBuilderDemo: ZIO[TerminalService & zio.http.Client, HttpError | TUIError, Unit] =
    for
      _        <- TerminalService.println("\n=== Request Builder Demo ===")
      // Build a custom request
      request  <- ZIO.succeed(
                    HttpRequest
                      .get("https://httpbin.org/headers")
                      .withHeader("User-Agent", "zio-tui/0.1.0")
                      .withHeader("X-Custom-Header", "demo-value")
                  )
      _        <- TerminalService.println(s"Request URL: ${request.url}")
      _        <- TerminalService.println(s"Headers: ${request.headers.keys.mkString(", ")}")
      response <- ZIO.scoped(ZHttp.request(request))
      _        <- TerminalService.println(s"Status: ${response.status}")
    yield ()

  /** Demonstrate HttpService with dependency injection. */
  def httpServiceDemo: ZIO[TerminalService & HttpService, HttpError | TUIError, Unit] =
    for
      _        <- TerminalService.println("\n=== HttpService Demo (with DI) ===")
      // Use HttpService from environment
      response <- HttpService.get("https://httpbin.org/uuid")
      _        <- TerminalService.println(s"UUID Response: ${response.body.take(100)}")
      // Display in a table widget
      widget   <- ZIO.succeed(
                    Widget.table(
                      Seq("Property", "Value").map(layoutz.Text(_)),
                      Seq(
                        Seq("Status", response.status.toString),
                        Seq("Success", response.isSuccess.toString),
                        Seq("Body Length", s"${response.body.length} bytes"),
                      ).map(_.map(layoutz.Text(_))),
                    )
                  )
      result   <- TerminalService.render(widget)
      _        <- TerminalService.println(result.output)
    yield ()

  /** Demonstrate polling with Schedule. */
  def pollingDemo: ZIO[TerminalService & zio.http.Client, HttpError | TUIError, Unit] =
    for
      _ <- TerminalService.println("\n=== Polling Demo ===")
      _ <- TerminalService.println("Polling httpbin.org/uuid every 2 seconds (3 times)...")
      // Poll endpoint every 2 seconds, take first 3 responses
      _ <- ZIO.scoped(
             ZHttp
               .poll(
                 url = "https://httpbin.org/uuid",
                 schedule = Schedule.spaced(2.seconds),
               )(response => s"[${java.time.LocalTime.now}] Status: ${response.status}")
               .take(3)
               .foreach(msg => TerminalService.println(msg))
           )
      _ <- TerminalService.println("Polling completed!")
    yield ()

  /** Demonstrate error handling. */
  def errorHandlingDemo: ZIO[TerminalService & zio.http.Client, TUIError, Unit] =
    for
      _ <- TerminalService.println("\n=== Error Handling Demo ===")
      _ <- ZIO.scoped(ZHttp
             .get("https://httpbin.org/status/404"))
             .tapError { error =>
               TerminalService.println(s"Caught error: $error")
             }
             .catchAll {
               case HttpError.InvalidResponse(status, _) =>
                 TerminalService.println(s"Server returned error status: $status")
               case HttpError.NetworkError(cause)        =>
                 TerminalService.println(s"Network error: $cause")
               case HttpError.RequestTimeout(url)        =>
                 TerminalService.println(s"Request timeout: $url")
               case other                                =>
                 TerminalService.println(s"Other error: $other")
             }
    yield ()

  /** Main application logic. */
  def program: ZIO[TerminalService & HttpService & zio.http.Client, HttpError | TUIError, Unit] =
    for
      _ <- TerminalService.println("╔════════════════════════════════════════╗")
      _ <- TerminalService.println("║    ZIO-TUI HTTP Integration Demo     ║")
      _ <- TerminalService.println("║   Combining HTTP with Terminal UI     ║")
      _ <- TerminalService.println("╚════════════════════════════════════════╝")
      _ <- simpleGetDemo
      _ <- postDemo
      _ <- requestBuilderDemo
      _ <- httpServiceDemo
      _ <- pollingDemo
      _ <- errorHandlingDemo
      _ <- TerminalService.println("\n✨ HTTP integration demo completed!")
    yield ()

  /** Application entry point with dependency injection. */
  def run: ZIO[Environment & (ZIOAppArgs & Scope), Any, Any] =
    program
      .provide(
        TerminalService.live,
        HttpService.live,
        zio.http.Client.default,
      )
      .catchAll { error =>
        ZIO.logError(s"Application failed with error: $error") *>
          ZIO.fail(error)
      }
      .exitCode
