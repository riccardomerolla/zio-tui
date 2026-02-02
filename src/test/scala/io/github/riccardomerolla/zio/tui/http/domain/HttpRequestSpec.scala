package io.github.riccardomerolla.zio.tui.http.domain

import zio.Scope
import zio.test.*
import zio.test.Assertion.*

object HttpRequestSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("HttpRequest")(
    suite("get")(
      test("creates GET request with URL") {
        val request = HttpRequest.get("https://example.com")
        assertTrue(
          request.method == Method.GET,
          request.url == "https://example.com",
          request.headers.isEmpty,
          request.body.isEmpty,
        )
      }
    ),
    suite("post")(
      test("creates POST request with URL and body") {
        val request = HttpRequest.post("https://example.com", "test body")
        assertTrue(
          request.method == Method.POST,
          request.url == "https://example.com",
          request.body.contains("test body"),
        )
      }
    ),
    suite("put")(
      test("creates PUT request with URL and body") {
        val request = HttpRequest.put("https://example.com", "updated data")
        assertTrue(
          request.method == Method.PUT,
          request.url == "https://example.com",
          request.body.contains("updated data"),
        )
      }
    ),
    suite("delete")(
      test("creates DELETE request with URL") {
        val request = HttpRequest.delete("https://example.com/resource")
        assertTrue(
          request.method == Method.DELETE,
          request.url == "https://example.com/resource",
          request.body.isEmpty,
        )
      }
    ),
    suite("patch")(
      test("creates PATCH request with URL and body") {
        val request = HttpRequest.patch("https://example.com", "partial update")
        assertTrue(
          request.method == Method.PATCH,
          request.url == "https://example.com",
          request.body.contains("partial update"),
        )
      }
    ),
    suite("withHeader")(
      test("adds single header to request") {
        val request = HttpRequest.get("https://example.com").withHeader("Authorization", "Bearer token")
        assertTrue(
          request.headers.contains("Authorization"),
          request.headers("Authorization") == "Bearer token",
        )
      },
      test("preserves existing headers when adding new one") {
        val request = HttpRequest
          .get("https://example.com")
          .withHeader("Content-Type", "application/json")
          .withHeader("Authorization", "Bearer token")
        assertTrue(
          request.headers.size == 2,
          request.headers.contains("Content-Type"),
          request.headers.contains("Authorization"),
        )
      },
    ),
    suite("withHeaders")(
      test("adds multiple headers to request") {
        val headers = Map("Content-Type" -> "application/json", "Accept" -> "application/json")
        val request = HttpRequest.get("https://example.com").withHeaders(headers)
        assertTrue(
          request.headers.size == 2,
          request.headers.contains("Content-Type"),
          request.headers.contains("Accept"),
        )
      },
      test("merges with existing headers") {
        val request = HttpRequest
          .get("https://example.com")
          .withHeader("Authorization", "Bearer token")
          .withHeaders(Map("Content-Type" -> "application/json"))
        assertTrue(
          request.headers.size == 2,
          request.headers.contains("Authorization"),
          request.headers.contains("Content-Type"),
        )
      },
    ),
    suite("withBody")(
      test("sets request body") {
        val request = HttpRequest.get("https://example.com").withBody("custom body")
        assertTrue(
          request.body.contains("custom body")
        )
      },
      test("replaces existing body") {
        val request = HttpRequest.post("https://example.com", "original").withBody("replaced")
        assertTrue(
          request.body.contains("replaced")
        )
      },
    ),
    suite("immutability")(
      test("original request is unchanged after modifications") {
        val original = HttpRequest.get("https://example.com")
        val modified = original.withHeader("X-Test", "value").withBody("body")
        assertTrue(
          original.headers.isEmpty,
          original.body.isEmpty,
          modified.headers.nonEmpty,
          modified.body.nonEmpty,
        )
      }
    ),
  )
