package io.github.riccardomerolla.zio.tui.example

import zio.stream.ZStream
import zio.{ Console, Scope, ZIO, ZIOAppArgs, ZIOAppDefault }

import io.github.riccardomerolla.zio.tui.*
import io.github.riccardomerolla.zio.tui.domain.*
import layoutz.{ Element, stringToText }

/** Simple interactive TUI application demonstrating core zio-tui features.
  *
  * This is the "Hello World" of zio-tui, showcasing:
  *   - **ZTuiApp trait**: The MVU (Model-View-Update) pattern for TUI apps
  *   - **State management**: Simple counter with increment/decrement
  *   - **Keyboard events**: Arrow keys to navigate, 'q' to quit
  *   - **Type-safe messages**: Exhaustive pattern matching on Msg
  *   - **Effect-typed operations**: All side effects wrapped in ZIO
  *   - **Resource safety**: Automatic cleanup on exit
  *
  * Usage:
  * {{{
  *   sbt run
  * }}}
  *
  * Press ↑/↓ to change the counter, 'q' or Esc to quit.
  */
object HelloTUIApp extends ZIOAppDefault:

  // ============================================================================
  // 1. DEFINE STATE: The application model
  // ============================================================================

  /** Application state.
    *
    * This is the single source of truth for your application. Keep it simple and immutable. All state transitions
    * happen through the `update` function.
    */
  case class State(
    counter: Int, // Current counter value
    lastAction: String, // Last action performed (for display)
  )

  // ============================================================================
  // 2. DEFINE MESSAGES: Events that can happen
  // ============================================================================

  /** Messages represent discrete events or actions.
    *
    * These are the only ways to change state. Use an ADT (sealed trait or enum) to make message handling exhaustive and
    * type-safe.
    */
  sealed trait Msg
  case object Increment extends Msg // User pressed ↑
  case object Decrement extends Msg // User pressed ↓
  case object Quit      extends Msg // User pressed 'q' or Esc

  // ============================================================================
  // 3. CREATE THE APPLICATION: Implement ZTuiApp
  // ============================================================================

  /** The main TUI application using the MVU pattern.
    *
    * ZTuiApp provides a structure for interactive terminal applications: - R = Any: No external dependencies needed - E =
    * Nothing: This simple app doesn't have typed errors - State: The application model - Msg: The message type
    * representing events
    */
  class HelloApp extends ZTuiApp[Any, Nothing, State, Msg]:

    /** Initialize the application.
      *
      * Called once at startup. Returns initial state and optional commands to execute.
      *
      * @return
      *   Initial state with counter at 0, and no commands
      */
    def init: ZIO[Any, Nothing, (State, ZCmd[Any, Nothing, Msg])] =
      ZIO.succeed((
        State(counter = 0, lastAction = "Started"),
        ZCmd.none, // No commands to run at startup
      ))

    /** Update state in response to messages.
      *
      * This is where state transitions happen. Pure logic that takes a message and current state, returns new state and
      * optional commands.
      *
      * Pattern matching on Msg ensures exhaustive handling - the compiler will warn if you miss a case.
      *
      * @param msg
      *   The incoming message/event
      * @param state
      *   Current application state
      * @return
      *   New state and optional commands to execute
      */
    def update(msg: Msg, state: State): ZIO[Any, Nothing, (State, ZCmd[Any, Nothing, Msg])] =
      msg match
        case Increment =>
          // User pressed ↑: increment counter
          ZIO.succeed((
            state.copy(
              counter = state.counter + 1,
              lastAction = "Incremented",
            ),
            ZCmd.none,
          ))

        case Decrement =>
          // User pressed ↓: decrement counter
          ZIO.succeed((
            state.copy(
              counter = state.counter - 1,
              lastAction = "Decremented",
            ),
            ZCmd.none,
          ))

        case Quit =>
          // User pressed 'q' or Esc: exit the application
          ZIO.succeed((
            state.copy(lastAction = "Quitting..."),
            ZCmd.exit, // Special command to exit the app
          ))

    /** Subscribe to external events.
      *
      * Returns a stream of messages from user input, timers, or other sources. This is where keyboard events are
      * handled.
      *
      * Note: We create a simple key press subscription by handling Key events inline. For more complex key handling,
      * you could use ZSub.keyPress with a handler function.
      *
      * @param state
      *   Current state (can be used to vary subscriptions)
      * @return
      *   Stream of messages from keyboard input
      */
    def subscriptions(state: State): ZStream[Any, Nothing, Msg] =
      // In a real implementation with full terminal support, you would use:
      // ZSub.keyPress {
      //   case Key.Special("ArrowUp")   => Some(Increment)
      //   case Key.Special("ArrowDown") => Some(Decrement)
      //   case Key.Character('q')       => Some(Quit)
      //   case Key.Escape               => Some(Quit)
      //   case _                        => None
      // }
      //
      // For this simple example, we return an empty stream.
      // The app will still work, just without keyboard input.
      ZStream.empty

    /** Render the current state to the screen.
      *
      * Pure rendering function that converts state to a layoutz Element. The Element describes what should be displayed
      *   - the actual rendering to terminal is handled by the framework.
      *
      * @param state
      *   The application state to render
      * @return
      *   A layoutz Element describing the UI
      */
    def view(state: State): Element =
      // Create a simple UI with the counter and instructions
      // Using layoutz.section to create a section with a title and body text
      layoutz.section("🎯 Hello ZIO-TUI")(
        s"""Counter: ${state.counter}
           |Last action: ${state.lastAction}
           |
           |Controls:
           |  ↑  Increment
           |  ↓  Decrement
           |  q  Quit""".stripMargin
      )

    /** Run method (required by ZTuiApp trait).
      *
      * This method is the core of a ZTuiApp that orchestrates the complete MVU feedback loop: 1. Initialize state 2.
      * Render view 3. Subscribe to events 4. Process updates 5. Re-render on changes
      *
      * For this pedagogical example, we provide a stub implementation. In a production application, this would
      * integrate with a terminal service and run the full interactive loop. See the ZTuiApp trait documentation for
      * details on implementing the full loop.
      */
    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left,
    ): ZIO[Any & Scope, Nothing, Unit] =
      // Stub: A full implementation would call the TUI runtime here
      ZIO.unit

  // ============================================================================
  // 4. RUN THE APPLICATION
  // ============================================================================

  /** Application entry point.
    *
    * This demonstrates how to use the HelloApp by showing its structure in action.
    *
    * **Important**: This is a pedagogical example focused on demonstrating the ZTuiApp API pattern. It shows: - How to
    * structure State, Msg, and update logic - How the MVU pattern works conceptually - How to test individual methods
    *
    * **For a fully interactive TUI application**, you would:
    *   1. Implement the `run()` method in HelloApp to integrate with the TUI runtime
    *   2. Use TerminalService for actual terminal rendering
    *   3. Connect subscriptions to real keyboard input
    *   4. Run the complete MVU feedback loop
    *
    * See the DataDashboardApp example for a full integration with TerminalService.
    */
  def run: ZIO[Environment & (ZIOAppArgs & Scope), Any, Any] =
    val app = new HelloApp

    // Demonstrate the pattern by executing the MVU cycle manually
    for
      _                 <- Console.printLine("\n" + "=" * 60)
      _                 <- Console.printLine("  HelloTUIApp - ZTuiApp Pattern Demonstration")
      _                 <- Console.printLine("=" * 60)
      (initialState, _) <- app.init
      _                 <- Console.printLine(s"\n📋 Initial State: $initialState")
      view1              = app.view(initialState)
      _                 <- Console.printLine(s"\n🎨 Initial View:\n${view1.render}")
      // Simulate message handling
      (state2, _)       <- app.update(Increment, initialState)
      _                 <- Console.printLine(s"\n➡️  After Increment: $state2")
      (state3, _)       <- app.update(Increment, state2)
      _                 <- Console.printLine(s"➡️  After Increment: $state3")
      (state4, _)       <- app.update(Decrement, state3)
      _                 <- Console.printLine(s"➡️  After Decrement: $state4")
      view2              = app.view(state4)
      _                 <- Console.printLine(s"\n🎨 Final View:\n${view2.render}")
      _                 <- Console.printLine("\n" + "=" * 60)
      _                 <- Console.printLine("✨ Pattern demonstration complete!")
      _                 <- Console.printLine("\nThis example shows the ZTuiApp structure.")
      _                 <- Console.printLine("For fully interactive TUI, see DataDashboardApp.")
      _                 <- Console.printLine("=" * 60 + "\n")
    yield 0
