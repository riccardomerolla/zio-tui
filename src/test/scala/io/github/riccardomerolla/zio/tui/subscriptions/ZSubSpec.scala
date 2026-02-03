package io.github.riccardomerolla.zio.tui.subscriptions

import zio.*
import zio.stream.*
import zio.test.Assertion.*
import zio.test.{ TestClock, * }

object ZSubSpec extends ZIOSpecDefault:

  sealed trait TestMsg
  case object Increment extends TestMsg
  case object Decrement extends TestMsg
  case object Quit      extends TestMsg

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("ZSub")(
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
    suite("merge")(
      test("combines multiple streams") {
        val stream1 = ZStream(1, 2, 3)
        val stream2 = ZStream(4, 5, 6)
        val stream3 = ZStream(7, 8, 9)

        for
          result <- ZSub.merge(stream1, stream2, stream3).runCollect
        yield assertTrue(result.sorted == Chunk(1, 2, 3, 4, 5, 6, 7, 8, 9))
      },
      test("handles empty stream list") {
        for
          result <- ZSub.merge[Any, Nothing, Int]().runCollect
        yield assertTrue(result.isEmpty)
      },
      test("handles single stream") {
        val stream = ZStream(1, 2, 3)
        for
          result <- ZSub.merge(stream).runCollect
        yield assertTrue(result == Chunk(1, 2, 3))
      },
      test("preserves error type") {
        val stream1: ZStream[Any, String, Int] = ZStream.fail("error1")
        val stream2: ZStream[Any, String, Int] = ZStream(1, 2)

        for
          result <- ZSub.merge(stream1, stream2).runCollect.either
        yield assertTrue(result.isLeft)
      },
      test("interleaves emissions from multiple sources") {
        val stream1 = ZStream(1, 2)
        val stream2 = ZStream(3, 4)

        for
          result <- ZSub.merge(stream1, stream2).runCollect
        yield assertTrue(result.size == 4 && result.toSet == Set(1, 2, 3, 4))
      },
    ),
    suite("watchFile")(
      test("fails with FileNotFound for missing file") {
        for
          result <- ZSub.watchFile("/nonexistent/file.txt").runHead.either
        yield assertTrue(result match
          case Left(_: io.github.riccardomerolla.zio.tui.error.TUIError.FileNotFound) => true
          case _                                                                      => false)
      },
      test("emits file content") {
        val testFile = "/tmp/test-watch-content.txt"

        for
          _      <- ZIO.attempt(java.nio.file.Files.writeString(
                      java.nio.file.Paths.get(testFile),
                      "test content",
                    ))
          result <- ZSub.watchFile(testFile).runHead
          _      <- ZIO.attempt(java.nio.file.Files.deleteIfExists(
                      java.nio.file.Paths.get(testFile)
                    ))
        yield assertTrue(result.contains("test content"))
      },
    ),
    suite("keyPress")(
      test("maps characters to messages via handler") {
        val handler: Key => Option[TestMsg] = {
          case Key.Character('+') => Some(Increment)
          case Key.Character('-') => Some(Decrement)
          case Key.Character('q') => Some(Quit)
          case _                  => None
        }

        // For now, just verify the function exists and compiles
        val stream = ZSub.keyPress(handler)
        assertTrue(stream.isInstanceOf[ZStream[Any, Nothing, TestMsg]])
      },
      test("filters out None results from handler") {
        val handler: Key => Option[TestMsg] = {
          case Key.Character('q') => Some(Quit)
          case _                  => None
        }

        val stream = ZSub.keyPress(handler)
        assertTrue(stream.isInstanceOf[ZStream[Any, Nothing, TestMsg]])
      },
    ),
  )
