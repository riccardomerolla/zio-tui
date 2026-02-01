package io.github.riccardomerolla.zio.tui

import zio.test.*

import io.github.riccardomerolla.zio.tui.error.TUIError

/** ZIO Test specification for TUIError.
  *
  * Tests cover:
  *   - All error cases can be created
  *   - Error messages contain relevant information
  *   - Pattern matching works correctly
  */
object TUIErrorSpec extends ZIOSpecDefault:

  def spec: Spec[TestEnvironment, Any] = suite("TUIError")(
    suite("InitializationFailed")(
      test("can be created with reason") {
        val error = TUIError.InitializationFailed("Could not initialize terminal")
        assertTrue(error match
          case TUIError.InitializationFailed(reason) => reason == "Could not initialize terminal"
          case _                                     => false)
      },
      test("is a TUIError") {
        val error: TUIError = TUIError.InitializationFailed("test")
        assertTrue(error.isInstanceOf[TUIError])
      },
    ),
    suite("RenderingFailed")(
      test("can be created with widget and cause") {
        val error = TUIError.RenderingFailed(widget = "TextWidget", cause = "Invalid UTF-8")
        assertTrue(error match
          case TUIError.RenderingFailed(w, c) => w == "TextWidget" && c == "Invalid UTF-8"
          case _                              => false)
      },
      test("stores widget name") {
        val error = TUIError.RenderingFailed("MyWidget", "error")
        assertTrue(error match
          case TUIError.RenderingFailed(widget, _) => widget == "MyWidget"
          case _                                   => false)
      },
      test("stores cause") {
        val error = TUIError.RenderingFailed("Widget", "reason")
        assertTrue(error match
          case TUIError.RenderingFailed(_, cause) => cause == "reason"
          case _                                  => false)
      },
    ),
    suite("TerminalClosed")(
      test("is a singleton case object") {
        val error1 = TUIError.TerminalClosed
        val error2 = TUIError.TerminalClosed
        assertTrue(error1 == error2)
      },
      test("can be pattern matched") {
        val error: TUIError = TUIError.TerminalClosed
        val matched = error match
          case TUIError.TerminalClosed => true
          case _                       => false
        assertTrue(matched)
      },
    ),
    suite("InvalidDimensions")(
      test("can be created with width, height, and reason") {
        val error = TUIError.InvalidDimensions(width = -10, height = 0, reason = "Negative dimensions")
        assertTrue(error match
          case TUIError.InvalidDimensions(w, h, r) => w == -10 && h == 0 && r == "Negative dimensions"
          case _                                   => false)
      },
      test("stores all parameters") {
        val error = TUIError.InvalidDimensions(100, 50, "Too large")
        assertTrue(error match
          case TUIError.InvalidDimensions(width, height, reason) =>
            width == 100 && height == 50 && reason == "Too large"
          case _                                                 => false)
      },
    ),
    suite("IOError")(
      test("can be created with operation and cause") {
        val error = TUIError.IOError(operation = "write", cause = "Permission denied")
        assertTrue(error match
          case TUIError.IOError(op, c) => op == "write" && c == "Permission denied"
          case _                       => false)
      },
      test("stores operation") {
        val error = TUIError.IOError("clear", "failed")
        assertTrue(error match
          case TUIError.IOError(operation, _) => operation == "clear"
          case _                              => false)
      },
      test("stores cause") {
        val error = TUIError.IOError("flush", "timeout")
        assertTrue(error match
          case TUIError.IOError(_, cause) => cause == "timeout"
          case _                          => false)
      },
    ),
    suite("pattern matching")(
      test("can match all error types exhaustively") {
        val errors: List[TUIError] = List(
          TUIError.InitializationFailed("test"),
          TUIError.RenderingFailed("widget", "cause"),
          TUIError.TerminalClosed,
          TUIError.InvalidDimensions(10, 10, "test"),
          TUIError.IOError("op", "cause"),
        )

        val matched = errors.map {
          case TUIError.InitializationFailed(_)    => "init"
          case TUIError.RenderingFailed(_, _)      => "render"
          case TUIError.TerminalClosed             => "closed"
          case TUIError.InvalidDimensions(_, _, _) => "dimensions"
          case TUIError.IOError(_, _)              => "io"
        }

        assertTrue(
          matched == List("init", "render", "closed", "dimensions", "io")
        )
      },
      test("different error instances are not equal") {
        val error1 = TUIError.IOError("op1", "cause1")
        val error2 = TUIError.IOError("op2", "cause2")
        assertTrue(error1 != error2)
      },
      test("same error instances are equal") {
        val error1 = TUIError.IOError("op", "cause")
        val error2 = TUIError.IOError("op", "cause")
        assertTrue(error1 == error2)
      },
    ),
  )
