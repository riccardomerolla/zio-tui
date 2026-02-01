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
      test("handles Exit command") {
        val messages = scala.collection.mutable.ListBuffer[String]()
        val runtime  = Runtime.default
        val cmd      = ZCmd.exit
        LayoutzBridge.convertCmd(cmd, (msg: String) => messages += msg, runtime)
        assertTrue(messages.isEmpty)
      },
      test("executes Effect command and calls onMessage") {
        val messages = scala.collection.mutable.ListBuffer[String]()
        val runtime  = Runtime.default
        val effect   = ZIO.succeed(42)
        val cmd      = ZCmd.effect(effect)(n => s"Result: $n")
        LayoutzBridge.convertCmd(cmd, (msg: String) => messages += msg, runtime)
        assertTrue(
          messages.length == 1,
          messages.head == "Result: 42",
        )
      },
      test("executes Effect command with ZIO transformation") {
        val messages = scala.collection.mutable.ListBuffer[Int]()
        val runtime  = Runtime.default
        val effect   = ZIO.succeed("10").map(_.toInt)
        val cmd      = ZCmd.effect(effect)(n => n * 2)
        LayoutzBridge.convertCmd(cmd, (msg: Int) => messages += msg, runtime)
        assertTrue(
          messages.length == 1,
          messages.head == 20,
        )
      },
      test("executes Fire command without error") {
        val runtime = Runtime.default
        val effect  = ZIO.unit
        val cmd     = ZCmd.fire(effect)
        LayoutzBridge.convertCmd(cmd, (_: String) => (), runtime)
        assertTrue(true)
      },
      test("executes Fire command with side effects") {
        for
          ref <- Ref.make(0)
          runtime = Runtime.default
          cmd     = ZCmd.fire(ref.update(_ + 1))
          _       = LayoutzBridge.convertCmd(cmd, (_: String) => (), runtime)
          value   <- ref.get
        yield assertTrue(value == 1)
      },
      test("executes Batch command sequentially") {
        val messages = scala.collection.mutable.ListBuffer[String]()
        val runtime  = Runtime.default
        val cmd      = ZCmd.batch(
          ZCmd.effect(ZIO.succeed(1))(n => s"msg$n"),
          ZCmd.effect(ZIO.succeed(2))(n => s"msg$n"),
          ZCmd.effect(ZIO.succeed(3))(n => s"msg$n"),
        )
        LayoutzBridge.convertCmd(cmd, (msg: String) => messages += msg, runtime)
        assertTrue(
          messages.length == 3,
          messages == List("msg1", "msg2", "msg3"),
        )
      },
      test("executes Batch with mixed command types") {
        val messages = scala.collection.mutable.ListBuffer[String]()
        val runtime  = Runtime.default
        val cmd      = ZCmd.batch(
          ZCmd.none,
          ZCmd.effect(ZIO.succeed("a"))(identity),
          ZCmd.fire(ZIO.unit),
          ZCmd.effect(ZIO.succeed("b"))(identity),
        )
        LayoutzBridge.convertCmd(cmd, (msg: String) => messages += msg, runtime)
        assertTrue(
          messages.length == 2,
          messages == List("a", "b"),
        )
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
      },
      test("returns command from update") {
        sealed trait Msg
        case object Inc extends Msg

        val app = new ZTuiApp[Any, Nothing, Int, Msg]:
          def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])]                         = ZIO.succeed((0, ZCmd.none))
          def update(msg: Msg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])] =
            ZIO.succeed((state + 1, ZCmd.exit))
          def subscriptions(state: Int): ZStream[Any, Nothing, Msg]                           = ZStream.empty
          def view(state: Int): Element                                                       = layoutz.Text(state.toString)
          def run(
            clearOnStart: Boolean = true,
            clearOnExit: Boolean = true,
            showQuitMessage: Boolean = false,
            alignment: layoutz.Alignment = layoutz.Alignment.Left,
          ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit

        val runtime = Runtime.default
        val result  = LayoutzBridge.updateApp(app, Inc, 0, runtime)
        assertTrue(result match
          case Right((newState, cmd)) => newState == 1 && cmd == ZCmd.Exit
          case _                      => false)
      },
    ),
    suite("getSubscriptions")(
      test("retrieves subscriptions from app") {
        sealed trait Msg
        case object Tick extends Msg

        val app = new ZTuiApp[Any, Nothing, Int, Msg]:
          def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])]                         = ZIO.succeed((0, ZCmd.none))
          def update(msg: Msg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])] = ZIO.succeed((state, ZCmd.none))
          def subscriptions(state: Int): ZStream[Any, Nothing, Msg]                           = ZStream.fromIterable(List.fill(state)(Tick))
          def view(state: Int): Element                                                       = layoutz.Text(state.toString)
          def run(
            clearOnStart: Boolean = true,
            clearOnExit: Boolean = true,
            showQuitMessage: Boolean = false,
            alignment: layoutz.Alignment = layoutz.Alignment.Left,
          ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit

        val runtime = Runtime.default
        val msgs    = LayoutzBridge.getSubscriptions(app, 3, runtime)
        assertTrue(
          msgs.length == 3,
          msgs.forall(_ == Tick),
        )
      },
      test("returns empty list for no subscriptions") {
        sealed trait Msg
        case object NoOp extends Msg

        val app = new ZTuiApp[Any, Nothing, String, Msg]:
          def init: ZIO[Any, Nothing, (String, ZCmd[Any, Nothing, Msg])]                         = ZIO.succeed(("", ZCmd.none))
          def update(msg: Msg, state: String): ZIO[Any, Nothing, (String, ZCmd[Any, Nothing, Msg])] = ZIO.succeed((state, ZCmd.none))
          def subscriptions(state: String): ZStream[Any, Nothing, Msg]                           = ZStream.empty
          def view(state: String): Element                                                       = layoutz.Text(state)
          def run(
            clearOnStart: Boolean = true,
            clearOnExit: Boolean = true,
            showQuitMessage: Boolean = false,
            alignment: layoutz.Alignment = layoutz.Alignment.Left,
          ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit

        val runtime = Runtime.default
        val msgs    = LayoutzBridge.getSubscriptions(app, "state", runtime)
        assertTrue(msgs.isEmpty)
      },
    ),
    suite("renderApp")(
      test("renders app state to Element") {
        sealed trait Msg
        case object NoOp extends Msg

        val app = new ZTuiApp[Any, Nothing, String, Msg]:
          def init: ZIO[Any, Nothing, (String, ZCmd[Any, Nothing, Msg])]                         = ZIO.succeed(("", ZCmd.none))
          def update(msg: Msg, state: String): ZIO[Any, Nothing, (String, ZCmd[Any, Nothing, Msg])] = ZIO.succeed((state, ZCmd.none))
          def subscriptions(state: String): ZStream[Any, Nothing, Msg]                           = ZStream.empty
          def view(state: String): Element                                                       = layoutz.Text(s"State: $state")
          def run(
            clearOnStart: Boolean = true,
            clearOnExit: Boolean = true,
            showQuitMessage: Boolean = false,
            alignment: layoutz.Alignment = layoutz.Alignment.Left,
          ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit

        val element = LayoutzBridge.renderApp(app, "test-state")
        assertTrue(element match
          case layoutz.Text(text) => text == "State: test-state"
          case _                  => false)
      },
      test("renders different states differently") {
        sealed trait Msg
        case object NoOp extends Msg

        val app = new ZTuiApp[Any, Nothing, Int, Msg]:
          def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])]                         = ZIO.succeed((0, ZCmd.none))
          def update(msg: Msg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, Msg])] = ZIO.succeed((state, ZCmd.none))
          def subscriptions(state: Int): ZStream[Any, Nothing, Msg]                           = ZStream.empty
          def view(state: Int): Element                                                       = layoutz.Text(s"Count: $state")
          def run(
            clearOnStart: Boolean = true,
            clearOnExit: Boolean = true,
            showQuitMessage: Boolean = false,
            alignment: layoutz.Alignment = layoutz.Alignment.Left,
          ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit

        val elem1 = LayoutzBridge.renderApp(app, 0)
        val elem2 = LayoutzBridge.renderApp(app, 42)
        assertTrue(
          elem1 != elem2,
          elem2 match
            case layoutz.Text(text) => text.contains("42")
            case _                  => false,
        )
      },
    ),
    suite("cleanupApp")(
      test("calls app onExit") {
        for
          ref <- Ref.make(false)
          app = new ZTuiApp[Any, Nothing, String, String]:
                  def init                                              = ZIO.succeed(("", ZCmd.none))
                  def update(msg: String, state: String)                = ZIO.succeed((state, ZCmd.none))
                  def subscriptions(state: String)                      = ZStream.empty
                  def view(state: String)                               = layoutz.Text(state)
                  override def onExit(state: String)                    = ref.set(true)
                  def run(
                    clearOnStart: Boolean = true,
                    clearOnExit: Boolean = true,
                    showQuitMessage: Boolean = false,
                    alignment: layoutz.Alignment = layoutz.Alignment.Left,
                  ) = ZIO.unit
          runtime = Runtime.default
          _       = LayoutzBridge.cleanupApp(app, "final", runtime)
          cleaned <- ref.get
        yield assertTrue(cleaned)
      },
      test("passes final state to onExit") {
        for
          ref <- Ref.make("")
          app = new ZTuiApp[Any, Nothing, String, String]:
                  def init                                              = ZIO.succeed(("", ZCmd.none))
                  def update(msg: String, state: String)                = ZIO.succeed((state, ZCmd.none))
                  def subscriptions(state: String)                      = ZStream.empty
                  def view(state: String)                               = layoutz.Text(state)
                  override def onExit(state: String)                    = ref.set(state)
                  def run(
                    clearOnStart: Boolean = true,
                    clearOnExit: Boolean = true,
                    showQuitMessage: Boolean = false,
                    alignment: layoutz.Alignment = layoutz.Alignment.Left,
                  ) = ZIO.unit
          runtime = Runtime.default
          _       = LayoutzBridge.cleanupApp(app, "my-final-state", runtime)
          state   <- ref.get
        yield assertTrue(state == "my-final-state")
      },
    ),
  )
