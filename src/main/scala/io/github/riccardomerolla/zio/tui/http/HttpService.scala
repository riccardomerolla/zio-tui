package io.github.riccardomerolla.zio.tui.http

import zio.*
import zio.http.Client
import zio.stream.*

import io.github.riccardomerolla.zio.tui.http.domain.*

/** Service for HTTP operations with dependency injection support.
  *
  * Provides effect-typed HTTP operations that can be composed with ZLayer for dependency injection and testing. All
  * operations are non-blocking and interruptible.
  *
  * For simple use cases without dependency injection, consider using [[ZHttp]] instead.
  */
trait HttpService:
  /** Execute an HTTP request.
    *
    * @param request
    *   The HTTP request to execute
    * @return
    *   Effect that produces an HttpResponse or HttpError
    */
  def request(request: HttpRequest): IO[HttpError, HttpResponse]

  /** Execute a GET request.
    *
    * @param url
    *   The URL to request
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect that produces an HttpResponse or HttpError
    */
  def get(url: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse]

  /** Execute a POST request.
    *
    * @param url
    *   The URL to request
    * @param body
    *   The request body
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect that produces an HttpResponse or HttpError
    */
  def post(url: String, body: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse]

  /** Execute a PUT request.
    *
    * @param url
    *   The URL to request
    * @param body
    *   The request body
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect that produces an HttpResponse or HttpError
    */
  def put(url: String, body: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse]

  /** Execute a DELETE request.
    *
    * @param url
    *   The URL to request
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect that produces an HttpResponse or HttpError
    */
  def delete(url: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse]

  /** Poll a URL periodically using a ZIO Schedule.
    *
    * Returns a stream of parsed values according to the provided schedule.
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
  def poll[A](url: String, schedule: Schedule[Any, Any, Any])(parse: HttpResponse => A): ZStream[Any, HttpError, A]

/** Live implementation of HttpService using ZIO HTTP client.
  */
final case class HttpServiceLive(client: Client) extends HttpService:

  override def request(request: HttpRequest): IO[HttpError, HttpResponse] =
    ZIO.scoped {
      ZHttp.request(request).provideSomeEnvironment[Scope](_.add(client))
    }

  override def get(url: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse] =
    request(HttpRequest.get(url).withHeaders(headers))

  override def post(url: String, body: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse] =
    request(HttpRequest.post(url, body).withHeaders(headers))

  override def put(url: String, body: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse] =
    request(HttpRequest.put(url, body).withHeaders(headers))

  override def delete(url: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse] =
    request(HttpRequest.delete(url).withHeaders(headers))

  override def poll[A](
    url: String,
    schedule: Schedule[Any, Any, Any],
  )(
    parse: HttpResponse => A
  ): Stream[HttpError, A] =
    ZStream
      .fromZIO(get(url))
      .map(parse)
      .repeat(schedule)

object HttpService:
  /** Access HttpService from environment and execute a request.
    *
    * @param request
    *   The HTTP request to execute
    * @return
    *   Effect requiring HttpService that produces HttpResponse
    */
  def request(request: HttpRequest): ZIO[HttpService, HttpError, HttpResponse] =
    ZIO.serviceWithZIO[HttpService](_.request(request))

  /** Access HttpService and execute a GET request.
    *
    * @param url
    *   The URL to request
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect requiring HttpService that produces HttpResponse
    */
  def get(url: String, headers: Map[String, String] = Map.empty): ZIO[HttpService, HttpError, HttpResponse] =
    ZIO.serviceWithZIO[HttpService](_.get(url, headers))

  /** Access HttpService and execute a POST request.
    *
    * @param url
    *   The URL to request
    * @param body
    *   The request body
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect requiring HttpService that produces HttpResponse
    */
  def post(
    url: String,
    body: String,
    headers: Map[String, String] = Map.empty,
  ): ZIO[HttpService, HttpError, HttpResponse] =
    ZIO.serviceWithZIO[HttpService](_.post(url, body, headers))

  /** Access HttpService and execute a PUT request.
    *
    * @param url
    *   The URL to request
    * @param body
    *   The request body
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect requiring HttpService that produces HttpResponse
    */
  def put(
    url: String,
    body: String,
    headers: Map[String, String] = Map.empty,
  ): ZIO[HttpService, HttpError, HttpResponse] =
    ZIO.serviceWithZIO[HttpService](_.put(url, body, headers))

  /** Access HttpService and execute a DELETE request.
    *
    * @param url
    *   The URL to request
    * @param headers
    *   Optional HTTP headers
    * @return
    *   Effect requiring HttpService that produces HttpResponse
    */
  def delete(url: String, headers: Map[String, String] = Map.empty): ZIO[HttpService, HttpError, HttpResponse] =
    ZIO.serviceWithZIO[HttpService](_.delete(url, headers))

  /** Live ZLayer for HttpService with default HTTP client.
    */
  val live: ZLayer[Any, Throwable, HttpService] =
    Client.default >>> ZLayer.fromFunction(HttpServiceLive.apply)

  /** Test/mock implementation returning predefined responses.
    *
    * Useful for testing without actual network I/O.
    *
    * @param responses
    *   Map of URLs to predefined responses
    * @param defaultResponse
    *   Default response for URLs not in the map
    */
  def test(
    responses: Map[String, HttpResponse] = Map.empty,
    defaultResponse: HttpResponse = HttpResponse(200, "", Map.empty),
  ): ULayer[HttpService] =
    ZLayer.succeed(new HttpService:
      override def request(request: HttpRequest): IO[HttpError, HttpResponse] =
        ZIO.succeed(responses.getOrElse(request.url, defaultResponse))

      override def get(url: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse] =
        ZIO.succeed(responses.getOrElse(url, defaultResponse))

      override def post(
        url: String,
        body: String,
        headers: Map[String, String] = Map.empty,
      ): IO[HttpError, HttpResponse] =
        ZIO.succeed(responses.getOrElse(url, defaultResponse))

      override def put(
        url: String,
        body: String,
        headers: Map[String, String] = Map.empty,
      ): IO[HttpError, HttpResponse] =
        ZIO.succeed(responses.getOrElse(url, defaultResponse))

      override def delete(url: String, headers: Map[String, String] = Map.empty): IO[HttpError, HttpResponse] =
        ZIO.succeed(responses.getOrElse(url, defaultResponse))

      override def poll[A](
        url: String,
        schedule: Schedule[Any, Any, Any],
      )(
        parse: HttpResponse => A
      ): Stream[HttpError, A] =
        ZStream
          .fromZIO(get(url))
          .map(parse)
          .repeat(schedule))
