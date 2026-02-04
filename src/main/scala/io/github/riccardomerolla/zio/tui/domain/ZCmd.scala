package io.github.riccardomerolla.zio.tui.domain

import zio.*

/** Sealed trait representing a command to be executed by the TUI runtime.
  *
  * A ZCmd is a composable abstraction for side effects that the application wants to perform in response to state
  * changes. Unlike raw ZIO effects, ZCmd provides semantic meaning about command execution:
  *
  *   - `None`: No operation needed
  *   - `Exit`: Terminate the application
  *   - `Effect`: Execute an effect and feed the result back as a message
  *   - `Fire`: Execute an effect for its side effects only, discarding the result
  *   - `Batch`: Execute multiple commands in sequence
  *
  * All commands are properly typed with environment and error channels, enabling full composability with ZIO.
  *
  * @tparam R
  *   The environment required for the command
  * @tparam E
  *   The error type
  * @tparam Msg
  *   The message type produced by the command
  */
sealed trait ZCmd[-R, +E, +Msg]:

  /** Batch this command with another.
    *
    * @param other
    *   The command to batch with
    * @return
    *   A batch command containing both commands
    */
  def ++[R2 <: R, E2 >: E, Msg2 >: Msg](other: ZCmd[R2, E2, Msg2]): ZCmd[R2, E2, Msg2] =
    ZCmd.batch(this, other)

object ZCmd:

  /** No operation. The command performs nothing.
    *
    * This is the identity element for batch operations.
    */
  case object None extends ZCmd[Any, Nothing, Nothing]

  /** Exit the application with success status.
    *
    * Signals that the TUI loop should terminate gracefully.
    */
  case object Exit extends ZCmd[Any, Nothing, Nothing]

  /** Execute an effect and feed its result back as a message.
    *
    * The effect is executed asynchronously and its result (or error) is converted to a message via `toMsg`.
    *
    * @tparam R
    *   The environment required by the effect
    * @tparam E
    *   The error type
    * @tparam A
    *   The value type produced by the effect
    * @tparam Msg
    *   The message type
    * @param zio
    *   The effect to execute
    * @param toMsg
    *   Function to convert the effect's result into a message
    */
  case class Effect[R, E, A, Msg](zio: ZIO[R, E, A], toMsg: A => Msg) extends ZCmd[R, E, Msg]

  /** Execute an effect purely for its side effects, discarding the result.
    *
    * This is useful for effects like logging, analytics, or cleanup operations where the result is not needed.
    *
    * @tparam R
    *   The environment required by the effect
    * @param effect
    *   The effect to execute (must not fail)
    */
  case class Fire[R](effect: ZIO[R, Nothing, Unit]) extends ZCmd[R, Nothing, Nothing]

  /** Batch multiple commands to be executed in sequence.
    *
    * Commands in a batch are executed one after another. The batch is empty if the list is empty.
    *
    * @tparam R
    *   The environment required by all commands
    * @tparam E
    *   The error type of all commands
    * @tparam Msg
    *   The message type of all commands
    * @param cmds
    *   List of commands to execute
    */
  case class Batch[R, E, Msg](cmds: List[ZCmd[R, E, Msg]]) extends ZCmd[R, E, Msg]

  /** Create a command that executes an effect and feeds its result back as a message.
    *
    * @param zio
    *   The effect to execute
    * @param toMsg
    *   Function to convert the effect's result into a message
    * @return
    *   A command that executes the effect
    */
  def effect[R, E, A, Msg](zio: ZIO[R, E, A])(toMsg: A => Msg): ZCmd[R, E, Msg] =
    Effect(zio, toMsg)

  /** Create a command that executes an effect for its side effects only.
    *
    * @param effect
    *   The effect to execute (must not fail)
    * @return
    *   A command that fires the effect
    */
  def fire[R](effect: ZIO[R, Nothing, Unit]): ZCmd[R, Nothing, Nothing] =
    Fire(effect)

  /** Create a batch command from multiple commands.
    *
    * @param cmds
    *   Commands to batch
    * @return
    *   A command that executes all commands in order
    */
  def batch[R, E, Msg](cmds: ZCmd[R, E, Msg]*): ZCmd[R, E, Msg] =
    val filtered = cmds.filter {
      case None => false
      case _    => true
    }.toList
    filtered.length match
      case 0 => None
      case 1 => filtered.head.asInstanceOf[ZCmd[R, E, Msg]]
      case _ => Batch(filtered)

  /** Create an exit command.
    *
    * @return
    *   A command that exits the application
    */
  def exit: ZCmd[Any, Nothing, Nothing] = Exit

  /** Create a no-op command.
    *
    * @return
    *   A command that does nothing
    */
  def none: ZCmd[Any, Nothing, Nothing] = None
