package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.test.*

import io.github.riccardomerolla.zio.tui.*
import io.github.riccardomerolla.zio.tui.example.CounterApp.*

object CounterAppSpec extends ZIOSpecDefault:

  def spec: Spec[TestEnvironment & Scope, Any] = suite("CounterApp")(
    suite("CounterState")(
      test("initializes with count 0") {
        val state = CounterState(0)
        assertTrue(state.count == 0)
      }
    ),
    suite("CounterMsg")(
      test("has all required message types") {
        val increment: CounterMsg = CounterMsg.Increment
        val decrement: CounterMsg = CounterMsg.Decrement
        val reset: CounterMsg     = CounterMsg.Reset
        val quit: CounterMsg      = CounterMsg.Quit
        assertTrue(
          increment == CounterMsg.Increment,
          decrement == CounterMsg.Decrement,
          reset == CounterMsg.Reset,
          quit == CounterMsg.Quit,
        )
      }
    ),
    suite("init")(
      test("starts with count 0 and no command") {
        val app = new CounterApp
        for
          (state, cmd) <- app.init
        yield assertTrue(
          state.count == 0,
          cmd == ZCmd.none,
        )
      }
    ),
    suite("update")(
      test("Increment increases count by 1") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(CounterMsg.Increment, CounterState(5))
        yield assertTrue(
          newState.count == 6,
          cmd == ZCmd.none,
        )
      },
      test("Decrement decreases count by 1") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(CounterMsg.Decrement, CounterState(5))
        yield assertTrue(
          newState.count == 4,
          cmd == ZCmd.none,
        )
      },
      test("Reset sets count to 0") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(CounterMsg.Reset, CounterState(42))
        yield assertTrue(
          newState.count == 0,
          cmd == ZCmd.none,
        )
      },
      test("Quit returns exit command") {
        val app = new CounterApp
        for
          (newState, cmd) <- app.update(CounterMsg.Quit, CounterState(10))
        yield assertTrue(
          newState.count == 10,
          cmd == ZCmd.Exit,
        )
      },
    ),
    suite("view")(
      test("renders current count") {
        val app     = new CounterApp
        val element = app.view(CounterState(42))
        assertTrue(element.isInstanceOf[layoutz.Element])
      }
    ),
  )
