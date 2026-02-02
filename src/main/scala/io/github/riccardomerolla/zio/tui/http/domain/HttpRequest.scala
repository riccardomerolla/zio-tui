package io.github.riccardomerolla.zio.tui.http.domain

/** Immutable HTTP request model.
  *
  * Represents an HTTP request with method, URL, headers, and optional body. This is an immutable data structure that
  * can be easily composed and modified.
  *
  * @param method
  *   The HTTP method to use
  * @param url
  *   The target URL
  * @param headers
  *   HTTP headers as key-value pairs
  * @param body
  *   Optional request body (for POST, PUT, PATCH)
  */
case class HttpRequest(
  method: Method,
  url: String,
  headers: Map[String, String] = Map.empty,
  body: Option[String] = None,
):
  /** Add a header to the request.
    *
    * @param key
    *   Header name
    * @param value
    *   Header value
    * @return
    *   New request with the header added
    */
  def withHeader(key: String, value: String): HttpRequest =
    copy(headers = headers + (key -> value))

  /** Add multiple headers to the request.
    *
    * @param newHeaders
    *   Headers to add
    * @return
    *   New request with the headers added
    */
  def withHeaders(newHeaders: Map[String, String]): HttpRequest =
    copy(headers = headers ++ newHeaders)

  /** Set the request body.
    *
    * @param content
    *   The body content
    * @return
    *   New request with the body set
    */
  def withBody(content: String): HttpRequest =
    copy(body = Some(content))

object HttpRequest:
  /** Create a GET request.
    *
    * @param url
    *   The target URL
    * @return
    *   A GET request
    */
  def get(url: String): HttpRequest =
    HttpRequest(Method.GET, url)

  /** Create a POST request.
    *
    * @param url
    *   The target URL
    * @param body
    *   The request body
    * @return
    *   A POST request
    */
  def post(url: String, body: String): HttpRequest =
    HttpRequest(Method.POST, url, body = Some(body))

  /** Create a PUT request.
    *
    * @param url
    *   The target URL
    * @param body
    *   The request body
    * @return
    *   A PUT request
    */
  def put(url: String, body: String): HttpRequest =
    HttpRequest(Method.PUT, url, body = Some(body))

  /** Create a DELETE request.
    *
    * @param url
    *   The target URL
    * @return
    *   A DELETE request
    */
  def delete(url: String): HttpRequest =
    HttpRequest(Method.DELETE, url)

  /** Create a PATCH request.
    *
    * @param url
    *   The target URL
    * @param body
    *   The request body
    * @return
    *   A PATCH request
    */
  def patch(url: String, body: String): HttpRequest =
    HttpRequest(Method.PATCH, url, body = Some(body))
