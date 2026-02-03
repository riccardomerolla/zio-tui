package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.stream.ZStream

import io.github.riccardomerolla.zio.tui.*
import io.github.riccardomerolla.zio.tui.domain.*
import io.github.riccardomerolla.zio.tui.subscriptions.*
import layoutz.Element

/** Counter application demonstrating The Elm Architecture pattern.
  *
  * This minimal example showcases:
  *   - State management with case class
  *   - Message-based updates (Increment, Decrement, Reset, Quit)
  *   - Keyboard subscriptions using ZSub.keyPress
  *   - Pure view rendering
  *
  * Run with: sbt "runMain io.github.riccardomerolla.zio.tui.example.CounterApp"
  */
object CounterApp extends ZIOAppDefault:

  case class CounterState(count: Int)

  enum CounterMsg:
    case Increment
    case Decrement
    case Reset
    case Quit

  class CounterApp extends ZTuiApp[Any, Nothing, CounterState, CounterMsg]:

    def init: ZIO[Any, Nothing, (CounterState, ZCmd[Any, Nothing, CounterMsg])] =
      ZIO.succeed((CounterState(0), ZCmd.none))

    def update(
      msg: CounterMsg,
      state: CounterState,
    ): ZIO[Any, Nothing, (CounterState, ZCmd[Any, Nothing, CounterMsg])] =
      msg match
        case CounterMsg.Increment => ZIO.succeed((CounterState(state.count + 1), ZCmd.none))
        case CounterMsg.Decrement => ZIO.succeed((CounterState(state.count - 1), ZCmd.none))
        case CounterMsg.Reset     => ZIO.succeed((CounterState(0), ZCmd.none))
        case CounterMsg.Quit      => ZIO.succeed((state, ZCmd.exit))

    def subscriptions(state: CounterState): ZStream[Any, Nothing, CounterMsg] =
      ZSub.keyPress {
        case Key.Character('+') => Some(CounterMsg.Increment)
        case Key.Character('-') => Some(CounterMsg.Decrement)
        case Key.Character('r') => Some(CounterMsg.Reset)
        case Key.Character('q') => Some(CounterMsg.Quit)
        case _                  => None
      }

    def view(state: CounterState): Element =
      layoutz.layout(
        layoutz.Text("=== ZIO Counter ==="),
        layoutz.Text(s"Count: ${state.count}"),
        layoutz.Text(""),
        layoutz.Text("Press '+' to increment"),
        layoutz.Text("Press '-' to decrement"),
        layoutz.Text("Press 'r' to reset"),
        layoutz.Text("Press 'q' to quit"),
      )

    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left,
    ): ZIO[Any & Scope, Nothing, Unit] =
      ZIO.unit // Placeholder - full implementation would integrate with layoutz runtime

  /** Demo application entry point.
    *
    * Demonstrates the Elm Architecture pattern by simulating a sequence of messages and showing how the state and view
    * update in response.
    */
  def run: ZIO[Any, Nothing, Unit] =
    for
      app               <- ZIO.succeed(new CounterApp)
      (initialState, _) <- app.init
      _                 <- Console.printLine(s"\nInitial view:\n${app.view(initialState).render}").orDie
      _                 <- ZIO.sleep(1.second)
      // Simulate increment
      (state1, _)       <- app.update(CounterMsg.Increment, initialState)
      _                 <- Console.printLine(s"\nAfter Increment:\n${app.view(state1).render}").orDie
      _                 <- ZIO.sleep(1.second)
      // Simulate increment again
      (state2, _)       <- app.update(CounterMsg.Increment, state1)
      _                 <- Console.printLine(s"\nAfter Increment:\n${app.view(state2).render}").orDie
      _                 <- ZIO.sleep(1.second)
      // Simulate decrement
      (state3, _)       <- app.update(CounterMsg.Decrement, state2)
      _                 <- Console.printLine(s"\nAfter Decrement:\n${app.view(state3).render}").orDie
      _                 <- ZIO.sleep(1.second)
      // Simulate reset
      (state4, _)       <- app.update(CounterMsg.Reset, state3)
      _                 <- Console.printLine(s"\nAfter Reset:\n${app.view(state4).render}").orDie
      _                 <- Console.printLine("\nDemo complete! This shows The Elm Architecture pattern in action.").orDie
    yield ()
