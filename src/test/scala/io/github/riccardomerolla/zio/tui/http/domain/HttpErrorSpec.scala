package io.github.riccardomerolla.zio.tui.http.domain

import zio.Scope
import zio.test.*

object HttpErrorSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("HttpError")(
    suite("NetworkError")(
      test("creates error with cause") {
        val error = HttpError.NetworkError("Connection refused")
        assertTrue(error.isInstanceOf[HttpError.NetworkError])
      },
      test("stores cause message") {
        val error = HttpError.NetworkError("Timeout")
        error match
          case HttpError.NetworkError(cause) => assertTrue(cause == "Timeout")
          case _                             => assertTrue(false)
      },
    ),
    suite("InvalidUrl")(
      test("creates error with URL") {
        val error = HttpError.InvalidUrl("not-a-valid-url")
        assertTrue(error.isInstanceOf[HttpError.InvalidUrl])
      },
      test("stores URL") {
        val error = HttpError.InvalidUrl("malformed://url")
        error match
          case HttpError.InvalidUrl(url) => assertTrue(url == "malformed://url")
          case _                         => assertTrue(false)
      },
    ),
    suite("RequestTimeout")(
      test("creates error with URL") {
        val error = HttpError.RequestTimeout("https://slow-api.com")
        assertTrue(error.isInstanceOf[HttpError.RequestTimeout])
      },
      test("stores URL") {
        val error = HttpError.RequestTimeout("https://example.com")
        error match
          case HttpError.RequestTimeout(url) => assertTrue(url == "https://example.com")
          case _                             => assertTrue(false)
      },
    ),
    suite("InvalidResponse")(
      test("creates error with status and body") {
        val error = HttpError.InvalidResponse(404, "Not Found")
        assertTrue(error.isInstanceOf[HttpError.InvalidResponse])
      },
      test("stores status code") {
        val error = HttpError.InvalidResponse(500, "Internal Server Error")
        error match
          case HttpError.InvalidResponse(status, _) => assertTrue(status == 500)
          case _                                    => assertTrue(false)
      },
      test("stores response body") {
        val error = HttpError.InvalidResponse(400, "Bad Request Body")
        error match
          case HttpError.InvalidResponse(_, body) => assertTrue(body == "Bad Request Body")
          case _                                  => assertTrue(false)
      },
    ),
    suite("DecodingFailed")(
      test("creates error with cause") {
        val error = HttpError.DecodingFailed("Invalid JSON")
        assertTrue(error.isInstanceOf[HttpError.DecodingFailed])
      },
      test("stores cause") {
        val error = HttpError.DecodingFailed("Unexpected token")
        error match
          case HttpError.DecodingFailed(cause) => assertTrue(cause == "Unexpected token")
          case _                               => assertTrue(false)
      },
    ),
    suite("pattern matching")(
      test("can match all error types") {
        val errors = List(
          HttpError.NetworkError("test"),
          HttpError.InvalidUrl("test"),
          HttpError.RequestTimeout("test"),
          HttpError.InvalidResponse(500, "test"),
          HttpError.DecodingFailed("test"),
        )

        val matched = errors.map {
          case HttpError.NetworkError(_)       => "network"
          case HttpError.InvalidUrl(_)         => "url"
          case HttpError.RequestTimeout(_)     => "timeout"
          case HttpError.InvalidResponse(_, _) => "response"
          case HttpError.DecodingFailed(_)     => "decoding"
        }

        assertTrue(
          matched == List("network", "url", "timeout", "response", "decoding")
        )
      }
    ),
  )
