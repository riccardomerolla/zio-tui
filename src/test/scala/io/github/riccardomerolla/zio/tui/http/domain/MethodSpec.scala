package io.github.riccardomerolla.zio.tui.http.domain

import zio.Scope
import zio.test.*

object MethodSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("Method")(
    suite("enum values")(
      test("GET method exists") {
        assertTrue(Method.GET.isInstanceOf[Method])
      },
      test("POST method exists") {
        assertTrue(Method.POST.isInstanceOf[Method])
      },
      test("PUT method exists") {
        assertTrue(Method.PUT.isInstanceOf[Method])
      },
      test("DELETE method exists") {
        assertTrue(Method.DELETE.isInstanceOf[Method])
      },
      test("PATCH method exists") {
        assertTrue(Method.PATCH.isInstanceOf[Method])
      },
      test("HEAD method exists") {
        assertTrue(Method.HEAD.isInstanceOf[Method])
      },
      test("OPTIONS method exists") {
        assertTrue(Method.OPTIONS.isInstanceOf[Method])
      },
    ),
    suite("pattern matching")(
      test("can match all methods") {
        val methods = List(
          Method.GET,
          Method.POST,
          Method.PUT,
          Method.DELETE,
          Method.PATCH,
          Method.HEAD,
          Method.OPTIONS,
        )

        val matched = methods.map {
          case Method.GET     => "get"
          case Method.POST    => "post"
          case Method.PUT     => "put"
          case Method.DELETE  => "delete"
          case Method.PATCH   => "patch"
          case Method.HEAD    => "head"
          case Method.OPTIONS => "options"
        }

        assertTrue(
          matched == List("get", "post", "put", "delete", "patch", "head", "options")
        )
      }
    ),
    suite("toString")(
      test("GET toString") {
        assertTrue(Method.GET.toString == "GET")
      },
      test("POST toString") {
        assertTrue(Method.POST.toString == "POST")
      },
      test("PUT toString") {
        assertTrue(Method.PUT.toString == "PUT")
      },
      test("DELETE toString") {
        assertTrue(Method.DELETE.toString == "DELETE")
      },
      test("PATCH toString") {
        assertTrue(Method.PATCH.toString == "PATCH")
      },
      test("HEAD toString") {
        assertTrue(Method.HEAD.toString == "HEAD")
      },
      test("OPTIONS toString") {
        assertTrue(Method.OPTIONS.toString == "OPTIONS")
      },
    ),
  )
