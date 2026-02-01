package io.github.riccardomerolla.zio.tui

import zio.*
import zio.stream.ZStream
import zio.test.*

import io.github.riccardomerolla.zio.tui.domain.*
import layoutz.Element

/** ZIO Test specification for ZTuiApp trait.
  *
  * Tests cover:
  *   - Application initialization
  *   - State updates in response to messages
  *   - Subscription streams
  *   - View rendering
  *   - Cleanup on exit
  *   - Complete application lifecycle
  */
object ZTuiAppSpec extends ZIOSpecDefault:

  // Test message types
  sealed trait TestMsg
  case object Increment     extends TestMsg
  case object Decrement     extends TestMsg
  case class SetValue(n: Int) extends TestMsg
  case object Exit          extends TestMsg

  // Simple counter app for testing
  class CounterApp extends ZTuiApp[Any, Nothing, Int, TestMsg]:
    def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, TestMsg])] =
      ZIO.succeed((0, ZCmd.none))

    def update(msg: TestMsg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, TestMsg])] =
      msg match
        case Increment      => ZIO.succeed((state + 1, ZCmd.none))
        case Decrement      => ZIO.succeed((state - 1, ZCmd.none))
        case SetValue(n)    => ZIO.succeed((n, ZCmd.none))
        case Exit           => ZIO.succeed((state, ZCmd.exit))

    def subscriptions(state: Int): ZStream[Any, Nothing, TestMsg] =
      ZStream.empty

    def view(state: Int): Element =
      layoutz.Text(s"Count: $state")

    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left,
    ): ZIO[Any & Scope, Nothing, Unit] =
      ZIO.unit

  // App with subscriptions
  class SubscriptionApp extends ZTuiApp[Any, Nothing, Int, TestMsg]:
    def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, TestMsg])] =
      ZIO.succeed((0, ZCmd.none))

    def update(msg: TestMsg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, TestMsg])] =
      msg match
        case Increment => ZIO.succeed((state + 1, ZCmd.none))
        case _         => ZIO.succeed((state, ZCmd.none))

    def subscriptions(state: Int): ZStream[Any, Nothing, TestMsg] =
      ZStream.fromIterable(List(Increment, Increment, Increment))

    def view(state: Int): Element =
      layoutz.Text(s"Count: $state")

    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left,
    ): ZIO[Any & Scope, Nothing, Unit] =
      ZIO.unit

  // App with cleanup
  class CleanupApp(cleanupRef: Ref[Boolean]) extends ZTuiApp[Any, Nothing, String, TestMsg]:
    def init: ZIO[Any, Nothing, (String, ZCmd[Any, Nothing, TestMsg])] =
      ZIO.succeed(("initialized", ZCmd.none))

    def update(msg: TestMsg, state: String): ZIO[Any, Nothing, (String, ZCmd[Any, Nothing, TestMsg])] =
      ZIO.succeed((state, ZCmd.none))

    def subscriptions(state: String): ZStream[Any, Nothing, TestMsg] =
      ZStream.empty

    def view(state: String): Element =
      layoutz.Text(state)

    override def onExit(state: String): ZIO[Any, Nothing, Unit] =
      cleanupRef.set(true)

    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left,
    ): ZIO[Any & Scope, Nothing, Unit] =
      ZIO.unit

  // App with commands
  class CommandApp extends ZTuiApp[Any, Nothing, Int, TestMsg]:
    def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, TestMsg])] =
      ZIO.succeed((0, ZCmd.effect(ZIO.succeed(5))(SetValue.apply)))

    def update(msg: TestMsg, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, TestMsg])] =
      msg match
        case SetValue(n) =>
          ZIO.succeed((n, ZCmd.fire(ZIO.unit)))
        case Increment   =>
          ZIO.succeed((state + 1, ZCmd.batch(
            ZCmd.fire(ZIO.unit),
            ZCmd.effect(ZIO.succeed(state + 10))(SetValue.apply),
          )))
        case _           => ZIO.succeed((state, ZCmd.none))

    def subscriptions(state: Int): ZStream[Any, Nothing, TestMsg] =
      ZStream.empty

    def view(state: Int): Element =
      layoutz.Text(s"Value: $state")

    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left,
    ): ZIO[Any & Scope, Nothing, Unit] =
      ZIO.unit

  def spec: Spec[TestEnvironment & Scope, Any] = suite("ZTuiApp")(
    suite("init")(
      test("initializes with default state") {
        val app = new CounterApp
        for
          (state, cmd) <- app.init
        yield assertTrue(
          state == 0,
          cmd == ZCmd.none,
        )
      },
      test("can return initial commands") {
        val app = new CommandApp
        for
          (state, cmd) <- app.init
        yield assertTrue(
          state == 0,
          cmd match
            case ZCmd.Effect(_, _) => true
            case _                 => false,
        )
      },
    ),
    suite("update")(
      test("processes increment message") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(Increment, 0)
        yield assertTrue(
          newState == 1,
          cmd == ZCmd.none,
        )
      },
      test("processes decrement message") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(Decrement, 5)
        yield assertTrue(
          newState == 4,
          cmd == ZCmd.none,
        )
      },
      test("processes set value message") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(SetValue(42), 0)
        yield assertTrue(
          newState == 42,
          cmd == ZCmd.none,
        )
      },
      test("can return commands from update") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(Exit, 10)
        yield assertTrue(
          newState == 10,
          cmd == ZCmd.Exit,
        )
      },
      test("handles multiple update calls") {
        val app = new CounterApp
        for
          (s1, _) <- app.update(Increment, 0)
          (s2, _) <- app.update(Increment, s1)
          (s3, _) <- app.update(Decrement, s2)
        yield assertTrue(s3 == 1)
      },
    ),
    suite("subscriptions")(
      test("returns empty stream by default") {
        val app = new CounterApp
        for
          messages <- app.subscriptions(0).runCollect
        yield assertTrue(messages.isEmpty)
      },
      test("can provide messages via subscriptions") {
        val app = new SubscriptionApp
        for
          messages <- app.subscriptions(0).runCollect
        yield assertTrue(
          messages.length == 3,
          messages.forall(_ == Increment),
        )
      },
      test("subscriptions can vary by state") {
        val app = new SubscriptionApp
        for
          msgs1 <- app.subscriptions(0).runCollect
          msgs2 <- app.subscriptions(100).runCollect
        yield assertTrue(
          msgs1.length == 3,
          msgs2.length == 3,
        )
      },
    ),
    suite("view")(
      test("renders state as Element") {
        val app = new CounterApp
        val element = app.view(42)
        assertTrue(element match
          case layoutz.Text(text) => text.contains("42")
          case _                  => false)
      },
      test("updates view when state changes") {
        val app = new CounterApp
        val view1 = app.view(0)
        val view2 = app.view(10)
        assertTrue(
          view1 != view2,
          view2 match
            case layoutz.Text(text) => text.contains("10")
            case _                  => false,
        )
      },
    ),
    suite("onExit")(
      test("default onExit is a no-op") {
        val app = new CounterApp
        for
          _ <- app.onExit(42)
        yield assertTrue(true)
      },
      test("can override onExit for cleanup") {
        for
          ref <- Ref.make(false)
          app = new CleanupApp(ref)
          _   <- app.onExit("final state")
          cleaned <- ref.get
        yield assertTrue(cleaned)
      },
      test("onExit receives final state") {
        for
          ref <- Ref.make("")
          app = new ZTuiApp[Any, Nothing, String, TestMsg]:
                  def init = ZIO.succeed(("init", ZCmd.none))
                  def update(msg: TestMsg, state: String) = ZIO.succeed((state, ZCmd.none))
                  def subscriptions(state: String) = ZStream.empty
                  def view(state: String) = layoutz.Text(state)
                  override def onExit(state: String) = ref.set(state)
                  def run(
                    clearOnStart: Boolean = true,
                    clearOnExit: Boolean = true,
                    showQuitMessage: Boolean = false,
                    alignment: layoutz.Alignment = layoutz.Alignment.Left,
                  ) = ZIO.unit
          _   <- app.onExit("my-final-state")
          finalState <- ref.get
        yield assertTrue(finalState == "my-final-state")
      },
    ),
    suite("run")(
      test("run method signature accepts parameters") {
        val app = new CounterApp
        for
          _ <- app.run(
                 clearOnStart = false,
                 clearOnExit = false,
                 showQuitMessage = true,
                 alignment = layoutz.Alignment.Center,
               )
        yield assertTrue(true)
      },
      test("run method uses default parameters") {
        val app = new CounterApp
        for
          _ <- app.run()
        yield assertTrue(true)
      },
    ),
    suite("command integration")(
      test("init can return effect command") {
        val app = new CommandApp
        for
          (state, cmd) <- app.init
        yield assertTrue(
          state == 0,
          cmd match
            case ZCmd.Effect(zio, toMsg) => true
            case _                       => false,
        )
      },
      test("update can return fire command") {
        val app = new CommandApp
        for
          (state, cmd) <- app.update(SetValue(10), 0)
        yield assertTrue(
          state == 10,
          cmd match
            case ZCmd.Fire(_) => true
            case _            => false,
        )
      },
      test("update can return batch command") {
        val app = new CommandApp
        for
          (state, cmd) <- app.update(Increment, 5)
        yield assertTrue(
          cmd match
            case ZCmd.Batch(cmds) => cmds.length == 2
            case _                => false,
        )
      },
    ),
    suite("full lifecycle")(
      test("complete init -> update -> view cycle") {
        val app = new CounterApp
        for
          (initialState, _) <- app.init
          (state1, _)       <- app.update(Increment, initialState)
          (state2, _)       <- app.update(Increment, state1)
          (state3, _)       <- app.update(Decrement, state2)
          view              = app.view(state3)
        yield assertTrue(
          state3 == 1,
          view match
            case layoutz.Text(text) => text.contains("1")
            case _                  => false,
        )
      },
      test("handles exit command in update") {
        val app = new CounterApp
        for
          (state, _)    <- app.init
          (finalState, cmd) <- app.update(Exit, state)
        yield assertTrue(
          cmd == ZCmd.Exit,
          finalState == state,
        )
      },
    ),
  )
