package io.github.riccardomerolla.zio.tui.http

import zio.*
import zio.test.*
import zio.test.Assertion.*

import io.github.riccardomerolla.zio.tui.http.domain.*

object HttpServiceSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("HttpService")(
    suite("get")(
      test("retrieves response for URL") {
        val testResponse = HttpResponse(200, """{"data": "test"}""", Map("Content-Type" -> "application/json"))
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com/data" -> testResponse)
        )

        for response <- HttpService.get("https://api.example.com/data").provide(testLayer)
        yield assertTrue(
          response.status == 200,
          response.body.contains("test"),
          response.contentType.contains("application/json"),
        )
      },
      test("includes custom headers in request") {
        val testResponse = HttpResponse(200, "ok", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com" -> testResponse)
        )

        val headers = Map("Authorization" -> "Bearer token", "Accept" -> "application/json")
        for response <- HttpService.get("https://api.example.com", headers).provide(testLayer)
        yield assertTrue(response.status == 200)
      },
    ),
    suite("post")(
      test("sends POST request with body") {
        val testResponse = HttpResponse(201, "created", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com/items" -> testResponse)
        )

        for
          response <- HttpService
                        .post("https://api.example.com/items", """{"name": "test"}""")
                        .provide(testLayer)
        yield assertTrue(
          response.status == 201,
          response.body == "created",
        )
      }
    ),
    suite("put")(
      test("sends PUT request with body") {
        val testResponse = HttpResponse(200, "updated", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com/items/1" -> testResponse)
        )

        for
          response <- HttpService
                        .put("https://api.example.com/items/1", """{"name": "updated"}""")
                        .provide(testLayer)
        yield assertTrue(
          response.status == 200,
          response.body == "updated",
        )
      }
    ),
    suite("delete")(
      test("sends DELETE request") {
        val testResponse = HttpResponse(204, "", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com/items/1" -> testResponse)
        )

        for response <- HttpService.delete("https://api.example.com/items/1").provide(testLayer)
        yield assertTrue(
          response.status == 204,
          response.body.isEmpty,
        )
      }
    ),
    suite("request")(
      test("executes custom HttpRequest") {
        val testResponse = HttpResponse(200, "custom", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com/custom" -> testResponse)
        )

        val customRequest = HttpRequest
          .get("https://api.example.com/custom")
          .withHeader("X-Custom", "value")

        for response <- HttpService.request(customRequest).provide(testLayer)
        yield assertTrue(
          response.status == 200,
          response.body == "custom",
        )
      }
    ),
    suite("test layer")(
      test("returns default response for unmapped URLs") {
        val defaultResponse = HttpResponse(200, "default", Map.empty)
        val testLayer       = HttpService.test(
          responses = Map("https://api.example.com/known" -> HttpResponse(200, "known", Map.empty)),
          defaultResponse = defaultResponse,
        )

        for response <- HttpService.get("https://api.example.com/unknown").provide(testLayer)
        yield assertTrue(
          response.status == 200,
          response.body == "default",
        )
      },
      test("allows testing without network I/O") {
        val testResponse = HttpResponse(200, "test data", Map("X-Test" -> "true"))
        val testLayer    = HttpService.test(
          responses = Map("https://test.com" -> testResponse)
        )

        for
          response1 <- HttpService.get("https://test.com").provide(testLayer)
          response2 <- HttpService.get("https://test.com").provide(testLayer)
        yield assertTrue(
          response1 == response2,
          response1.header("X-Test").contains("true"),
        )
      },
    ),
    suite("poll")(
      test("creates stream of responses") {
        val testResponse = HttpResponse(200, "polled", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com/status" -> testResponse)
        )

        for
          service <- ZIO.service[HttpService].provide(testLayer)
          results <- service
                       .poll("https://api.example.com/status", Schedule.recurs(2))(_.body)
                       .runCollect
        yield assertTrue(
          results.size == 3, // Initial + 2 recurs
          results.forall(_ == "polled"),
        )
      },
      test("applies transformation function to responses") {
        val testResponse = HttpResponse(200, "42", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://api.example.com/number" -> testResponse)
        )

        for
          service <- ZIO.service[HttpService].provide(testLayer)
          results <- service
                       .poll("https://api.example.com/number", Schedule.recurs(1))(response =>
                         response.body.toInt * 2
                       )
                       .runCollect
        yield assertTrue(
          results.size == 2,
          results.forall(_ == 84),
        )
      },
    ),
    suite("accessor methods")(
      test("HttpService.get accesses service from environment") {
        val testResponse = HttpResponse(200, "accessed", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://example.com" -> testResponse)
        )

        for response <- HttpService.get("https://example.com").provide(testLayer)
        yield assertTrue(response.body == "accessed")
      },
      test("HttpService.request accesses service from environment") {
        val testResponse = HttpResponse(200, "accessed", Map.empty)
        val testLayer    = HttpService.test(
          responses = Map("https://example.com" -> testResponse)
        )

        val request = HttpRequest.get("https://example.com")
        for response <- HttpService.request(request).provide(testLayer)
        yield assertTrue(response.body == "accessed")
      },
    ),
  )
