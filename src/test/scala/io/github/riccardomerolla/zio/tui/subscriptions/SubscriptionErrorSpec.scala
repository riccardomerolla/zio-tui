package io.github.riccardomerolla.zio.tui.subscriptions

import io.github.riccardomerolla.zio.tui.error.TUIError
import zio.test.*
import zio.test.Assertion.*

object SubscriptionErrorSpec extends ZIOSpecDefault:

  def spec = suite("SubscriptionError")(
    suite("FileNotFound")(
      test("stores the file path") {
        val error = SubscriptionError.FileNotFound("/path/to/file")
        assertTrue(error.path == "/path/to/file")
      },
      test("is a TUIError") {
        val error: TUIError = SubscriptionError.FileNotFound("/path")
        assertTrue(error.isInstanceOf[TUIError])
      },
    ),
    suite("IOError")(
      test("stores message and cause") {
        val cause = new RuntimeException("disk full")
        val error = SubscriptionError.IOError("failed to read", cause)
        assertTrue(error.operation == "failed to read" && error.cause.contains("disk full"))
      },
      test("is a TUIError") {
        val error: TUIError = SubscriptionError.IOError("msg", new Exception())
        assertTrue(error.isInstanceOf[TUIError])
      },
    ),
    suite("TerminalReadError")(
      test("stores the cause") {
        val cause = new RuntimeException("terminal closed")
        val error = SubscriptionError.TerminalReadError(cause)
        assertTrue(error.cause == cause)
      },
      test("is a TUIError") {
        val error: TUIError = SubscriptionError.TerminalReadError(new Exception())
        assertTrue(error.isInstanceOf[TUIError])
      },
    ),
    suite("pattern matching")(
      test("can match on FileNotFound") {
        val error: TUIError = SubscriptionError.FileNotFound("/missing")
        val result = error match
          case TUIError.FileNotFound(path) => s"not found: $path"
          case _                           => "other"
        assertTrue(result == "not found: /missing")
      },
    ),
  )
