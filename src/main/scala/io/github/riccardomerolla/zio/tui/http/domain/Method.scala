package io.github.riccardomerolla.zio.tui.http.domain

/** HTTP method enumeration.
  *
  * Represents the standard HTTP methods used in requests.
  */
enum Method:
  /** HTTP GET method - retrieve a resource */
  case GET

  /** HTTP POST method - create or submit data */
  case POST

  /** HTTP PUT method - update a resource */
  case PUT

  /** HTTP DELETE method - delete a resource */
  case DELETE

  /** HTTP PATCH method - partially update a resource */
  case PATCH

  /** HTTP HEAD method - retrieve headers only */
  case HEAD

  /** HTTP OPTIONS method - retrieve supported methods */
  case OPTIONS
