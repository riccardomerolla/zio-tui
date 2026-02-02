package io.github.riccardomerolla.zio.tui.subscriptions

import io.github.riccardomerolla.zio.tui.error.TUIError

/** Typed errors for subscription operations.
  *
  * SubscriptionError represents domain-specific errors that can occur during subscription operations like file
  * watching, keyboard input, etc. All errors extend TUIError for type alignment with ZTuiApp.
  *
  * Note: This is a type alias to specific TUIError cases. The actual error definitions are in TUIError enum.
  */
type SubscriptionError = TUIError.FileNotFound | TUIError.TerminalReadError | TUIError.IOError

object SubscriptionError:
  /** File was not found during file watching.
    *
    * @param path
    *   The path to the file that was not found
    */
  def FileNotFound(path: String): TUIError.FileNotFound =
    TUIError.FileNotFound(path)

  /** IO operation failed during subscription.
    *
    * @param message
    *   Description of the operation that failed
    * @param cause
    *   The underlying exception
    */
  def IOError(message: String, cause: Throwable): TUIError.IOError =
    TUIError.IOError(message, cause.getMessage)

  /** Terminal read operation failed.
    *
    * @param cause
    *   The underlying exception
    */
  def TerminalReadError(cause: Throwable): TUIError.TerminalReadError =
    TUIError.TerminalReadError(cause)
