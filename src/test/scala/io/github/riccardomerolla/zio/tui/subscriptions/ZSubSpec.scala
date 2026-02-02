package io.github.riccardomerolla.zio.tui.subscriptions

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestClock

object ZSubSpec extends ZIOSpecDefault:

  def spec = suite("ZSub")(
    suite("tick")(
      test("emits at regular intervals") {
        for
          fiber  <- ZSub.tick(1.second).take(3).runCollect.fork
          _      <- TestClock.adjust(3.seconds)
          result <- fiber.join
        yield assertTrue(result.size == 3)
      },
      test("emits Unit values") {
        for
          fiber  <- ZSub.tick(500.millis).take(2).runCollect.fork
          _      <- TestClock.adjust(1.second)
          result <- fiber.join
        yield assertTrue(result == Chunk((), ()))
      },
      test("respects the interval duration") {
        for
          fiber  <- ZSub.tick(2.seconds).take(2).runCollect.fork
          _      <- TestClock.adjust(4.seconds)
          result <- fiber.join
        yield assertTrue(result.size == 2)
      },
    ),
  )
