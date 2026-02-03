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
  */
object CounterApp:

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
      state: CounterState
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
        layoutz.Text("Press 'q' to quit")
      )

    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left
    ): ZIO[Any & Scope, Nothing, Unit] =
      ZIO.unit // Placeholder - full implementation would integrate with layoutz runtime
