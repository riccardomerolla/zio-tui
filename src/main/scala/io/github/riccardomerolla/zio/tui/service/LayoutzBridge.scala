package io.github.riccardomerolla.zio.tui.service

import zio.*
import zio.stream.ZStream

import io.github.riccardomerolla.zio.tui.domain.*

/** Error type for bridge layer operations. */
sealed trait LayoutzBridgeError
object LayoutzBridgeError:
  case object InitializationFailed extends LayoutzBridgeError
  case object UpdateFailed         extends LayoutzBridgeError
  case object SubscriptionFailed   extends LayoutzBridgeError
  case object ExecutionDefect      extends LayoutzBridgeError

/** Bridge layer for converting ZIO abstractions to layoutz equivalents.
  *
  * This service handles the impedance mismatch between ZIO's effect-oriented programming model and layoutz's
  * callback-based model. It uses `Runtime` to safely execute ZIO effects within layoutz's synchronous context.
  *
  * The bridge maintains proper error handling, logging, and resource cleanup throughout the conversion process.
  */
object LayoutzBridge:

  /** Execute a ZIO effect unsafely with proper error handling.
    *
    * @param effect
    *   The effect to execute
    * @param runtime
    *   The ZIO runtime
    * @return
    *   The result of the effect execution, or an error
    */
  private def unsafeRunOption[R, E, A](effect: ZIO[R, E, A], runtime: Runtime[R]): Option[A] =
    try
      Unsafe.unsafe { implicit unsafe =>
        val exit = runtime.unsafe.run(effect)
        exit match
          case Exit.Success(a)     => Some(a)
          case Exit.Failure(cause) =>
            cause.failureOrCause match
              case Left(_)  =>
                scala.Console.err.println("Effect failed")
                None
              case Right(_) =>
                scala.Console.err.println("Defect encountered in effect")
                None
      }
    catch
      case ex: Throwable =>
        scala.Console.err.println(s"Unexpected error executing effect: ${ex.getMessage}")
        ex.printStackTrace()
        None

  /** Convert a ZCmd to a layoutz Cmd that can be executed by the runtime.
    *
    * This function transforms ZIO-based commands into layoutz's callback-based command model. It handles:
    *   - Effect execution via the Runtime
    *   - Message conversion for Effect commands
    *   - Fire-and-forget semantics for Fire commands
    *   - Proper error handling with logging
    *
    * @param zCmd
    *   The ZIO command to convert
    * @param onMessage
    *   Callback to handle produced messages
    * @param runtime
    *   The ZIO runtime for executing effects
    * @tparam R
    *   Environment required by the effect
    * @tparam E
    *   Error type of the effect
    * @tparam Msg
    *   Message type produced by the command
    */
  def convertCmd[R, E, Msg](
    zCmd: ZCmd[R, E, Msg],
    onMessage: Msg => Unit,
    runtime: Runtime[R],
  ): Unit =
    (zCmd: @unchecked) match
      case ZCmd.None                         =>
        ()
      case ZCmd.Exit                         =>
        ()
      case effect: ZCmd.Effect[R, E, a, Msg] =>
        // Execute the effect and convert result to message
        val zio: ZIO[R, E, Msg] = effect.zio.map(effect.toMsg)
        unsafeRunOption[R, E, Msg](zio, runtime).foreach(onMessage)
      case fire: ZCmd.Fire[R]                =>
        // Execute fire-and-forget effect
        unsafeRunOption[R, Nothing, Unit](fire.effect, runtime)
      case batch: ZCmd.Batch[R, E, Msg]      =>
        // Execute batch of commands sequentially
        batch.cmds.foreach(cmd => convertCmd(cmd, onMessage, runtime))

  /** Convert a ZStream to a sequence of messages.
    *
    * This function bridges ZStream's streaming model to layoutz's subscription model. It collects all messages from the
    * stream and returns them for processing.
    *
    * Note: For long-running streams, this may block. Consider using fiber-based execution for interactive streams.
    *
    * @param stream
    *   The ZStream to convert
    * @param runtime
    *   The ZIO runtime for executing effects
    * @tparam R
    *   Environment required by the stream
    * @tparam E
    *   Error type of the stream
    * @tparam Msg
    *   Message type emitted by the stream
    * @return
    *   A list of messages from the stream or empty list on error
    */
  def convertSubscriptions[R, E, Msg](
    stream: ZStream[R, E, Msg],
    runtime: Runtime[R],
  ): List[Msg] =
    val collectEffect: ZIO[R, E, Chunk[Msg]] = stream.runCollect
    unsafeRunOption(collectEffect, runtime).map(_.toList).getOrElse(List.empty)

  /** Initialize a ZTuiApp and return the initial state and command.
    *
    * @param app
    *   The ZTuiApp to initialize
    * @param runtime
    *   The ZIO runtime
    * @return
    *   Either an error or the initial state with command
    */
  def initializeApp[R, E, State, Msg](
    app: ZTuiApp[R, E, State, Msg],
    runtime: Runtime[R],
  ): Either[LayoutzBridgeError | E, (State, ZCmd[R, E, Msg])] =
    unsafeRunOption(app.init, runtime) match
      case Some(result) => Right(result)
      case None         => Left(LayoutzBridgeError.InitializationFailed)

  /** Process a message through the app's update function.
    *
    * @param app
    *   The ZTuiApp
    * @param msg
    *   The message to process
    * @param state
    *   The current state
    * @param runtime
    *   The ZIO runtime
    * @return
    *   Either an error or the new state with command
    */
  def updateApp[R, E, State, Msg](
    app: ZTuiApp[R, E, State, Msg],
    msg: Msg,
    state: State,
    runtime: Runtime[R],
  ): Either[LayoutzBridgeError | E, (State, ZCmd[R, E, Msg])] =
    unsafeRunOption(app.update(msg, state), runtime) match
      case Some(result) => Right(result)
      case None         => Left(LayoutzBridgeError.UpdateFailed)

  /** Get subscriptions for the current state.
    *
    * @param app
    *   The ZTuiApp
    * @param state
    *   The current state
    * @param runtime
    *   The ZIO runtime
    * @return
    *   A list of messages from subscriptions
    */
  def getSubscriptions[R, E, State, Msg](
    app: ZTuiApp[R, E, State, Msg],
    state: State,
    runtime: Runtime[R],
  ): List[Msg] =
    convertSubscriptions(app.subscriptions(state), runtime)

  /** Render the current state.
    *
    * @param app
    *   The ZTuiApp
    * @param state
    *   The current state
    * @return
    *   A layoutz Element
    */
  def renderApp[R, E, State, Msg](
    app: ZTuiApp[R, E, State, Msg],
    state: State,
  ): layoutz.Element =
    app.view(state)

  /** Handle app cleanup on exit.
    *
    * @param app
    *   The ZTuiApp
    * @param state
    *   The final state
    * @param runtime
    *   The ZIO runtime
    */
  def cleanupApp[R, E, State, Msg](
    app: ZTuiApp[R, E, State, Msg],
    state: State,
    runtime: Runtime[R],
  ): Unit =
    unsafeRunOption(app.onExit(state), runtime)
