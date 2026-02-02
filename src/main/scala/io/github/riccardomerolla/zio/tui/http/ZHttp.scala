package io.github.riccardomerolla.zio.tui.http

import zio.*
import zio.http.*
import zio.stream.*

import io.github.riccardomerolla.zio.tui.http.domain.*

/** Simple utility object for HTTP operations.
  *
  * Provides convenient methods for common HTTP operations without requiring dependency injection. For production
  * applications that need proper service layers and testing, use [[HttpService]] instead.
  *
  * ==Example Usage==
  *
  * {{{
  * import io.github.riccardomerolla.zio.tui.http.*
  *
  * val program = for
  *   response <- ZHttp.get("https://api.example.com/data")
  *   _        <- Console.printLine(s"Status: \${response.status}")
  *   _        <- Console.printLine(s"Body: \${response.body}")
  * yield ()
  * }}}
  */
object ZHttp:
  /** Execute an HTTP request.
    *
    * @param request
    *   The HTTP request to execute
    * @return
    *   ZIO effect that produces an HttpResponse or HttpError
    */
  def request(request: domain.HttpRequest): ZIO[Client & Scope, HttpError, HttpResponse] =
    for
      // Convert our domain Method to ZIO HTTP Method
      zioMethod <- convertMethod(request.method)

      // Build ZIO HTTP Request
      zioRequest = Request(
                     method = zioMethod,
                     url = URL.decode(request.url).getOrElse(URL.empty),
                     headers = Headers(request.headers.map { case (k, v) => Header.Custom(k, v) }.toList),
                     body =
                       request.body.fold(Body.empty)(s => Body.fromString(s, java.nio.charset.StandardCharsets.UTF_8)),
                   )

      // Execute request
      response <- Client
                    .request(zioRequest)
                    .mapError(err => HttpError.NetworkError(err.getMessage))
                    .timeoutFail(HttpError.RequestTimeout(request.url))(30.seconds)

      // Convert response body to string
      bodyString <- response.body.asString
                      .mapError(err => HttpError.DecodingFailed(err.getMessage))

      // Convert ZIO HTTP headers to our Map
      headersMap = response.headers.toList.map(h => h.headerName -> h.renderedValue).toMap

      // Build our domain HttpResponse
      httpResponse = HttpResponse(
                       status = response.status.code,
                       body = bodyString,
                       headers = headersMap,
                     )

      // Check for HTTP error status codes
      _ <- ZIO.when(!httpResponse.isSuccess)(
             ZIO.fail(HttpError.InvalidResponse(httpResponse.status, httpResponse.body))
           )
    yield httpResponse

  /** Execute a GET request.
    *
    * @param url
    *   The URL to request
    * @param headers
    *   Optional HTTP headers
    * @return
    *   ZIO effect that produces an HttpResponse or HttpError
    */
  def get(url: String, headers: Map[String, String] = Map.empty): ZIO[Client & Scope, HttpError, HttpResponse] =
    request(domain.HttpRequest.get(url).withHeaders(headers))

  /** Execute a POST request.
    *
    * @param url
    *   The URL to request
    * @param body
    *   The request body
    * @param headers
    *   Optional HTTP headers
    * @return
    *   ZIO effect that produces an HttpResponse or HttpError
    */
  def post(
    url: String,
    body: String,
    headers: Map[String, String] = Map.empty,
  ): ZIO[Client & Scope, HttpError, HttpResponse] =
    request(domain.HttpRequest.post(url, body).withHeaders(headers))

  /** Execute a PUT request.
    *
    * @param url
    *   The URL to request
    * @param body
    *   The request body
    * @param headers
    *   Optional HTTP headers
    * @return
    *   ZIO effect that produces an HttpResponse or HttpError
    */
  def put(
    url: String,
    body: String,
    headers: Map[String, String] = Map.empty,
  ): ZIO[Client & Scope, HttpError, HttpResponse] =
    request(domain.HttpRequest.put(url, body).withHeaders(headers))

  /** Execute a DELETE request.
    *
    * @param url
    *   The URL to request
    * @param headers
    *   Optional HTTP headers
    * @return
    *   ZIO effect that produces an HttpResponse or HttpError
    */
  def delete(url: String, headers: Map[String, String] = Map.empty): ZIO[Client & Scope, HttpError, HttpResponse] =
    request(domain.HttpRequest.delete(url).withHeaders(headers))

  /** Poll a URL periodically using a ZIO Schedule.
    *
    * Returns a stream of responses according to the provided schedule. The schedule controls retry logic, backoff, and
    * repetition.
    *
    * ==Example==
    *
    * {{{
    * import zio.*
    * import io.github.riccardomerolla.zio.tui.http.*
    *
    * // Poll every 5 seconds
    * val stream = ZHttp.poll(
    *   url = "https://api.example.com/status",
    *   schedule = Schedule.spaced(5.seconds)
    * )(identity)
    *
    * stream.foreach(response => Console.printLine(s"Status: \${response.status}"))
    * }}}
    *
    * @param url
    *   The URL to poll
    * @param schedule
    *   ZIO Schedule controlling the polling behavior
    * @param parse
    *   Function to transform the HttpResponse into desired type A
    * @tparam A
    *   The result type after parsing
    * @return
    *   A ZStream that emits parsed values according to the schedule
    */
  def poll[A](
    url: String,
    schedule: Schedule[Any, Any, Any],
  )(
    parse: HttpResponse => A
  ): ZStream[Client & Scope, HttpError, A] =
    ZStream
      .fromZIO(get(url))
      .map(parse)
      .repeat(schedule)

  /** Convert our domain Method to ZIO HTTP Method. */
  private def convertMethod(method: domain.Method): ZIO[Any, HttpError, zio.http.Method] =
    method match
      case domain.Method.GET     => ZIO.succeed(zio.http.Method.GET)
      case domain.Method.POST    => ZIO.succeed(zio.http.Method.POST)
      case domain.Method.PUT     => ZIO.succeed(zio.http.Method.PUT)
      case domain.Method.DELETE  => ZIO.succeed(zio.http.Method.DELETE)
      case domain.Method.PATCH   => ZIO.succeed(zio.http.Method.PATCH)
      case domain.Method.HEAD    => ZIO.succeed(zio.http.Method.HEAD)
      case domain.Method.OPTIONS => ZIO.succeed(zio.http.Method.OPTIONS)
