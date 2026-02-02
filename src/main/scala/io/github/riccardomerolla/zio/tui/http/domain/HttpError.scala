package io.github.riccardomerolla.zio.tui.http.domain

/** Typed errors for HTTP operations.
  *
  * All HTTP errors are explicitly typed and never thrown as exceptions. This enables exhaustive pattern matching and
  * compile-time guarantees.
  */
enum HttpError:
  /** Network-level error occurred during the request.
    *
    * @param cause
    *   Description of the network failure
    */
  case NetworkError(cause: String)

  /** The provided URL is invalid or malformed.
    *
    * @param url
    *   The invalid URL that was provided
    */
  case InvalidUrl(url: String)

  /** The HTTP request timed out.
    *
    * @param url
    *   The URL that timed out
    */
  case RequestTimeout(url: String)

  /** The server returned an error response.
    *
    * @param status
    *   The HTTP status code returned
    * @param body
    *   The response body (may contain error details)
    */
  case InvalidResponse(status: Int, body: String)

  /** Failed to decode or parse the response.
    *
    * @param cause
    *   Description of the decoding failure
    */
  case DecodingFailed(cause: String)
