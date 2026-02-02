package io.github.riccardomerolla.zio.tui.http.domain

import zio.test.*
import zio.test.Assertion.*
import zio.Scope

object HttpResponseSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("HttpResponse")(
    suite("isSuccess")(
      test("returns true for 2xx status codes") {
        assertTrue(
          HttpResponse(200, "", Map.empty).isSuccess,
          HttpResponse(201, "", Map.empty).isSuccess,
          HttpResponse(204, "", Map.empty).isSuccess,
          HttpResponse(299, "", Map.empty).isSuccess,
        )
      },
      test("returns false for non-2xx status codes") {
        assertTrue(
          !HttpResponse(199, "", Map.empty).isSuccess,
          !HttpResponse(300, "", Map.empty).isSuccess,
          !HttpResponse(400, "", Map.empty).isSuccess,
          !HttpResponse(500, "", Map.empty).isSuccess,
        )
      },
    ),
    suite("isClientError")(
      test("returns true for 4xx status codes") {
        assertTrue(
          HttpResponse(400, "", Map.empty).isClientError,
          HttpResponse(404, "", Map.empty).isClientError,
          HttpResponse(403, "", Map.empty).isClientError,
          HttpResponse(499, "", Map.empty).isClientError,
        )
      },
      test("returns false for non-4xx status codes") {
        assertTrue(
          !HttpResponse(200, "", Map.empty).isClientError,
          !HttpResponse(399, "", Map.empty).isClientError,
          !HttpResponse(500, "", Map.empty).isClientError,
        )
      },
    ),
    suite("isServerError")(
      test("returns true for 5xx status codes") {
        assertTrue(
          HttpResponse(500, "", Map.empty).isServerError,
          HttpResponse(502, "", Map.empty).isServerError,
          HttpResponse(503, "", Map.empty).isServerError,
          HttpResponse(599, "", Map.empty).isServerError,
        )
      },
      test("returns false for non-5xx status codes") {
        assertTrue(
          !HttpResponse(200, "", Map.empty).isServerError,
          !HttpResponse(400, "", Map.empty).isServerError,
          !HttpResponse(499, "", Map.empty).isServerError,
          !HttpResponse(600, "", Map.empty).isServerError,
        )
      },
    ),
    suite("header")(
      test("retrieves header value by exact name") {
        val response = HttpResponse(200, "", Map("Content-Type" -> "application/json"))
        assertTrue(
          response.header("Content-Type").contains("application/json")
        )
      },
      test("retrieves header value case-insensitively") {
        val response = HttpResponse(200, "", Map("Content-Type" -> "application/json"))
        assertTrue(
          response.header("content-type").contains("application/json"),
          response.header("CONTENT-TYPE").contains("application/json"),
          response.header("CoNtEnT-TyPe").contains("application/json"),
        )
      },
      test("returns None for missing header") {
        val response = HttpResponse(200, "", Map("Content-Type" -> "application/json"))
        assertTrue(
          response.header("Authorization").isEmpty
        )
      },
    ),
    suite("contentType")(
      test("retrieves Content-Type header") {
        val response = HttpResponse(200, "", Map("Content-Type" -> "text/html"))
        assertTrue(
          response.contentType.contains("text/html")
        )
      },
      test("returns None when Content-Type is missing") {
        val response = HttpResponse(200, "", Map.empty)
        assertTrue(
          response.contentType.isEmpty
        )
      },
      test("retrieves Content-Type case-insensitively") {
        val response = HttpResponse(200, "", Map("content-type" -> "application/xml"))
        assertTrue(
          response.contentType.contains("application/xml")
        )
      },
    ),
    suite("construction")(
      test("creates response with all fields") {
        val headers  = Map("X-Custom" -> "value", "Content-Length" -> "42")
        val response = HttpResponse(201, "created", headers)
        assertTrue(
          response.status == 201,
          response.body == "created",
          response.headers == headers,
        )
      }
    ),
  )
