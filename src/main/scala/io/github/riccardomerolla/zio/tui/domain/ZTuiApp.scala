package io.github.riccardomerolla.zio.tui.domain

import zio.*
import zio.stream.ZStream

import layoutz.Element

/** Core application abstraction for ZIO-based TUI applications.
  *
  * ZTuiApp wraps layoutz's LayoutzApp with ZIO effects, following the MVU (Model-View-Update) pattern but with
  * effect-oriented programming:
  *
  *   - `State` is the application model
  *   - `Msg` represents discrete user actions or external events
  *   - `init` initializes the state and returns any initial commands
  *   - `update` transforms state in response to messages and returns new state with commands
  *   - `subscriptions` provide event streams (user input, timers, network events, etc.)
  *   - `view` renders the current state to the screen
  *   - `run` orchestrates the entire feedback loop
  *
  * All effects are properly typed with error channels, enabling exhaustive error handling and type safety.
  *
  * @tparam R
  *   The environment required by this application (services, config, etc.)
  * @tparam E
  *   The error type for application effects
  * @tparam State
  *   The application state
  * @tparam Msg
  *   The message type representing events/actions
  */
trait ZTuiApp[R, E, State, Msg]:

  /** Initialize the application.
    *
    * Called once at startup to set up initial state and any startup commands.
    *
    * @return
    *   Effect that produces initial state and optional startup commands
    */
  def init: ZIO[R, E, (State, ZCmd[R, E, Msg])]

  /** Update application state in response to a message.
    *
    * Pure state transition logic with support for side effects via commands.
    *
    * @param msg
    *   The incoming message/event
    * @param state
    *   The current application state
    * @return
    *   Effect that produces new state and optional follow-up commands
    */
  def update(msg: Msg, state: State): ZIO[R, E, (State, ZCmd[R, E, Msg])]

  /** Subscribe to external events and user input.
    *
    * Returns a stream of messages that should be processed by the update function. This is where you subscribe to:
    *   - User input events
    *   - Timer/schedule events
    *   - Network events
    *   - Any other external events
    *
    * @param state
    *   The current application state (can be used to determine what to subscribe to)
    * @return
    *   A stream of messages from external sources
    */
  def subscriptions(state: State): ZStream[R, E, Msg]

  /** Render the current state to the screen.
    *
    * Pure rendering function that produces an Element (from layoutz) representing the UI.
    *
    * @param state
    *   The application state to render
    * @return
    *   A layoutz Element representing the UI
    */
  def view(state: State): Element

  /** Optional cleanup hook called on application exit.
    *
    * Use this to perform cleanup operations like closing resources, saving state, etc.
    *
    * @param state
    *   The final application state
    * @return
    *   Effect that performs cleanup (errors are ignored)
    */
  def onExit(state: State): ZIO[R, Nothing, Unit] = ZIO.unit

  /** Run the complete TUI application.
    *
    * Orchestrates the entire feedback loop:
    *   1. Initialize state and render
    *   2. Subscribe to messages
    *   3. Process messages through update
    *   4. Re-render on state changes
    *   5. Handle cleanup on exit
    *
    * Requires a Scope for proper resource management of the terminal and subscriptions.
    *
    * @param clearOnStart
    *   Whether to clear the terminal at startup (default: true)
    * @param clearOnExit
    *   Whether to clear the terminal on exit (default: true)
    * @param showQuitMessage
    *   Whether to show a quit message before exiting (default: false)
    * @param alignment
    *   Alignment for rendering (default: Left)
    * @return
    *   Effect that runs the TUI loop until interrupted or failed
    */
  def run(
    clearOnStart: Boolean = true,
    clearOnExit: Boolean = true,
    showQuitMessage: Boolean = false,
    alignment: layoutz.Alignment = layoutz.Alignment.Left,
  ): ZIO[R & Scope, E, Unit]
