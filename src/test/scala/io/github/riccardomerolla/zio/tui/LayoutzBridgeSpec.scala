package io.github.riccardomerolla.zio.tui

import zio.*
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.*

import io.github.riccardomerolla.zio.tui.domain.*
import io.github.riccardomerolla.zio.tui.service.LayoutzBridge
import layoutz.Element

object LayoutzBridgeSpec extends ZIOSpecDefault:

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("LayoutzBridge")(
    suite("convertCmd")(
      test("handles None command") {
        val messages = scala.collection.mutable.ListBuffer[String]()
        val runtime  = Runtime.default
        val cmd      = ZCmd.none
        LayoutzBridge.convertCmd(cmd, (msg: String) => messages += msg, runtime)
        assertTrue(messages.isEmpty)
      },
      test("executes Fire command without error") {
        val runtime = Runtime.default
        val effect  = ZIO.unit
        val cmd     = ZCmd.fire(effect)
        LayoutzBridge.convertCmd(cmd, (_: String) => (), runtime)
        assertTrue(true)
      },
    ),
    suite("convertSubscriptions")(
      test("converts stream to list of messages") {
        val stream   = ZStream.fromIterable(List(1, 2, 3))
        val runtime  = Runtime.default
        val messages = LayoutzBridge.convertSubscriptions(stream, runtime)
        assertTrue(messages == List(1, 2, 3))
      },
      test("returns empty list on empty stream") {
        val stream: ZStream[Any, Nothing, String] = ZStream.empty
        val runtime                               = Runtime.default
        val messages                              = LayoutzBridge.convertSubscriptions(stream, runtime)
        assertTrue(messages.isEmpty)
      },
    ),
    suite("initializeApp")(
      test("initializes ZTuiApp") {
        sealed trait Msg
        case object Init extends Msg

        val app = new ZTuiApp[Any, Nothing, Int, Msg]:
          def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])]                         = ZIO.succeed((0, ZCmd.none))
          def update(msg: Msg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])] =
            ZIO.succeed((state + 1, ZCmd.none))
          def subscriptions(state: Int): ZStream[Any, Nothing, Msg]                           = ZStream.empty
          def view(state: Int): Element                                                       = layoutz.Text(state.toString)
          def run(
            clearOnStart: Boolean = true,
            clearOnExit: Boolean = true,
            showQuitMessage: Boolean = false,
            alignment: layoutz.Alignment = layoutz.Alignment.Left,
          ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit

        val runtime = Runtime.default
        val result  = LayoutzBridge.initializeApp(app, runtime)
        assertTrue(result == Right((0, ZCmd.none)))
      }
    ),
    suite("updateApp")(
      test("processes message") {
        sealed trait Msg
        case object Inc extends Msg

        val app = new ZTuiApp[Any, Nothing, Int, Msg]:
          def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])]                         = ZIO.succeed((0, ZCmd.none))
          def update(msg: Msg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])] =
            ZIO.succeed((state + 1, ZCmd.none))
          def subscriptions(state: Int): ZStream[Any, Nothing, Msg]                           = ZStream.empty
          def view(state: Int): Element                                                       = layoutz.Text(state.toString)
          def run(
            clearOnStart: Boolean = true,
            clearOnExit: Boolean = true,
            showQuitMessage: Boolean = false,
            alignment: layoutz.Alignment = layoutz.Alignment.Left,
          ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit

        val runtime = Runtime.default
        val result  = LayoutzBridge.updateApp(app, Inc, 42, runtime)
        assertTrue(result match
          case Right((newState, _)) => newState == 43
          case _                    => false)
      }
    ),
  )
