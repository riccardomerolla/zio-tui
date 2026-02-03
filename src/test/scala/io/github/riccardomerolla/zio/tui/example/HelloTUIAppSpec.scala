package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.test.*

import io.github.riccardomerolla.zio.tui.domain.*
import layoutz.Element

/** Test specification for HelloTUIApp.
  *
  * These tests demonstrate how to test ZTuiApp implementations by testing each method independently.
  */
object HelloTUIAppSpec extends ZIOSpecDefault:

  import HelloTUIApp.*

  def spec: Spec[TestEnvironment & Scope, Any] = suite("HelloTUIApp")(
    suite("HelloApp")(
      suite("init")(
        test("initializes with counter at 0") {
          val app = new HelloApp
          for
            (state, cmd) <- app.init
          yield assertTrue(
            state.counter == 0,
            state.lastAction == "Started",
            cmd == ZCmd.none,
          )
        }
      ),
      suite("update")(
        test("increments counter on Increment message") {
          val app          = new HelloApp
          val currentState = State(counter = 5, lastAction = "Previous")
          for
            (newState, cmd) <- app.update(Increment, currentState)
          yield assertTrue(
            newState.counter == 6,
            newState.lastAction == "Incremented",
            cmd == ZCmd.none,
          )
        },
        test("decrements counter on Decrement message") {
          val app          = new HelloApp
          val currentState = State(counter = 5, lastAction = "Previous")
          for
            (newState, cmd) <- app.update(Decrement, currentState)
          yield assertTrue(
            newState.counter == 4,
            newState.lastAction == "Decremented",
            cmd == ZCmd.none,
          )
        },
        test("handles negative counter values") {
          val app          = new HelloApp
          val currentState = State(counter = -5, lastAction = "Previous")
          for
            (newState, _) <- app.update(Decrement, currentState)
          yield assertTrue(newState.counter == -6)
        },
        test("returns exit command on Quit message") {
          val app          = new HelloApp
          val currentState = State(counter = 10, lastAction = "Previous")
          for
            (newState, cmd) <- app.update(Quit, currentState)
          yield assertTrue(
            newState.counter == 10, // State unchanged
            newState.lastAction == "Quitting...",
            cmd == ZCmd.exit,
          )
        },
        test("handles multiple updates in sequence") {
          val app = new HelloApp
          for
            (s1, _) <- app.update(Increment, State(0, "Start"))
            (s2, _) <- app.update(Increment, s1)
            (s3, _) <- app.update(Decrement, s2)
          yield assertTrue(s3.counter == 1)
        },
      ),
      suite("subscriptions")(
        test("returns empty stream by default") {
          val app = new HelloApp
          for
            messages <- app.subscriptions(State(0, "Test")).runCollect
          yield assertTrue(messages.isEmpty)
        }
      ),
      suite("view")(
        test("renders state as Element") {
          val app   = new HelloApp
          val state = State(counter = 42, lastAction = "Test")
          val view  = app.view(state)
          assertTrue(view match
            case _: Element => true
            case _          => false)
        },
        test("view content reflects state") {
          val app    = new HelloApp
          val state1 = State(counter = 10, lastAction = "Test1")
          val state2 = State(counter = 20, lastAction = "Test2")
          val view1  = app.view(state1)
          val view2  = app.view(state2)
          // Views should be different for different states
          assertTrue(view1 != view2)
        },
      ),
      suite("complete lifecycle")(
        test("full init -> update -> view cycle") {
          val app = new HelloApp
          for
            (initial, _) <- app.init
            (state1, _)  <- app.update(Increment, initial)
            (state2, _)  <- app.update(Increment, state1)
            (state3, _)  <- app.update(Decrement, state2)
            view          = app.view(state3)
          yield assertTrue(
            state3.counter == 1,
            view match
              case _: Element => true
              case _          => false,
          )
        }
      ),
    ),
    suite("State")(
      test("is immutable") {
        val state1 = State(counter = 5, lastAction = "Test")
        val state2 = state1.copy(counter = 10)
        assertTrue(
          state1.counter == 5,  // Original unchanged
          state2.counter == 10, // Copy has new value
          state1.lastAction == "Test",
          state2.lastAction == "Test",
        )
      }
    ),
    suite("Msg")(
      test("sealed trait ensures exhaustive matching") {
        // This test verifies that we can match on all message types
        val messages: List[Msg] = List(Increment, Decrement, Quit)
        val results             = messages.map {
          case Increment => "inc"
          case Decrement => "dec"
          case Quit      => "quit"
        }
        assertTrue(
          results == List("inc", "dec", "quit")
        )
      }
    ),
  )
