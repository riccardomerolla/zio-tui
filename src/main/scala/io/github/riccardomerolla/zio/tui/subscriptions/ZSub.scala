package io.github.riccardomerolla.zio.tui.subscriptions

import zio.*
import zio.stream.*

/** Factory methods for creating ZIO Stream-based subscriptions.
  *
  * ZSub provides ergonomic helpers for common TUI subscription patterns: timer ticks, file monitoring, keyboard input,
  * and stream composition. All subscriptions return `ZStream[R, E, A]` for composability with ZTuiApp.
  *
  * Example usage:
  * {{{
  * def subscriptions(state: State): ZStream[Any, TUIError, Msg] =
  *   ZSub.merge(
  *     ZSub.tick(1.second).map(_ => Msg.Tick),
  *     ZSub.keyPress {
  *       case Key.Character('q') => Some(Msg.Quit)
  *       case _ => None
  *     }
  *   )
  * }}}
  */
object ZSub:

  /** Create a subscription that emits Unit at regular intervals.
    *
    * Useful for periodic tasks like refreshing data, updating clocks, or polling external state. The stream never
    * fails and emits infinitely until interrupted.
    *
    * Backpressure: Natural rate limiting via the fixed schedule ensures bounded production.
    *
    * @param interval
    *   Duration between emissions
    * @return
    *   A stream that emits Unit at the specified interval
    *
    * @example
    *   {{{
    * ZSub.tick(1.second).map(_ => Msg.Refresh)
    *   }}}
    */
  def tick(interval: Duration): ZStream[Any, Nothing, Unit] =
    ZStream.repeatWithSchedule(ZIO.unit, Schedule.fixed(interval))
