package io.github.riccardomerolla.zio.tui.http.domain

/** Immutable HTTP response model.
  *
  * Represents the essential parts of an HTTP response - status code, body, and headers. This is a simplified model that
  * covers the common use cases while keeping the API clean.
  *
  * @param status
  *   The HTTP status code (e.g., 200, 404, 500)
  * @param body
  *   The response body as a string
  * @param headers
  *   HTTP response headers as key-value pairs
  */
case class HttpResponse(
  status: Int,
  body: String,
  headers: Map[String, String],
):
  /** Check if the response represents a successful HTTP status (2xx).
    *
    * @return
    *   true if status is between 200 and 299
    */
  def isSuccess: Boolean = status >= 200 && status < 300

  /** Check if the response represents a client error (4xx).
    *
    * @return
    *   true if status is between 400 and 499
    */
  def isClientError: Boolean = status >= 400 && status < 500

  /** Check if the response represents a server error (5xx).
    *
    * @return
    *   true if status is between 500 and 599
    */
  def isServerError: Boolean = status >= 500 && status < 600

  /** Get a specific header value by name (case-insensitive).
    *
    * @param name
    *   The header name
    * @return
    *   The header value, if present
    */
  def header(name: String): Option[String] =
    headers.find((k, _) => k.equalsIgnoreCase(name)).map(_._2)

  /** Get the Content-Type header, if present.
    *
    * @return
    *   The Content-Type value
    */
  def contentType: Option[String] = header("Content-Type")
