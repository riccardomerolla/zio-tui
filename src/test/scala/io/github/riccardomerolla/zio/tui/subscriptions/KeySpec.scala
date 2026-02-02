package io.github.riccardomerolla.zio.tui.subscriptions

import zio.Scope
import zio.test.*
import zio.test.Assertion.*

object KeySpec extends ZIOSpecDefault:

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("Key")(
    suite("Character")(
      test("wraps a regular character") {
        val key = Key.Character('a')
        assertTrue(key match
          case Key.Character('a') => true
          case _                  => false)
      },
      test("wraps digits") {
        val key = Key.Character('5')
        assertTrue(key match
          case Key.Character('5') => true
          case _                  => false)
      },
    ),
    suite("Special")(
      test("wraps special key names") {
        val key = Key.Special("ArrowUp")
        assertTrue(key match
          case Key.Special("ArrowUp") => true
          case _                      => false)
      }
    ),
    suite("Control")(
      test("wraps control characters") {
        val key = Key.Control('c')
        assertTrue(key match
          case Key.Control('c') => true
          case _                => false)
      }
    ),
    suite("case objects")(
      test("Enter is a singleton") {
        assertTrue(Key.Enter == Key.Enter)
      },
      test("Escape is a singleton") {
        assertTrue(Key.Escape == Key.Escape)
      },
      test("Backspace is a singleton") {
        assertTrue(Key.Backspace == Key.Backspace)
      },
      test("Tab is a singleton") {
        assertTrue(Key.Tab == Key.Tab)
      },
    ),
    suite("pattern matching")(
      test("can match on Character") {
        val key: Key = Key.Character('q')
        val result   = key match
          case Key.Character('q') => "quit"
          case _                  => "other"
        assertTrue(result == "quit")
      },
      test("can match on Control") {
        val key: Key = Key.Control('c')
        val result   = key match
          case Key.Control('c') => "interrupt"
          case _                => "other"
        assertTrue(result == "interrupt")
      },
      test("can match on case objects") {
        val key: Key = Key.Enter
        val result   = key match
          case Key.Enter => "submit"
          case _         => "other"
        assertTrue(result == "submit")
      },
    ),
  )
