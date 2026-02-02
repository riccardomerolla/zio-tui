package io.github.riccardomerolla.zio.tui.subscriptions

/** Typed representation of keyboard input events.
  *
  * Key provides a type-safe ADT for terminal keyboard input, avoiding raw strings or integers. This enables exhaustive
  * pattern matching and compile-time guarantees when handling user input.
  *
  * Example usage:
  * {{{
  * val handler: Key => Option[Msg] = {
  *   case Key.Character('q')  => Some(Msg.Quit)
  *   case Key.Control('c')    => Some(Msg.Quit)
  *   case Key.Enter           => Some(Msg.Submit)
  *   case Key.Special("ArrowUp") => Some(Msg.MoveUp)
  *   case _                   => None
  * }
  * }}}
  */
sealed trait Key

object Key:
  /** A regular printable character.
    *
    * @param char
    *   The character that was pressed
    */
  case class Character(char: Char) extends Key

  /** A special named key (function keys, arrow keys, etc.).
    *
    * Common special key names:
    *   - "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"
    *   - "F1" through "F12"
    *   - "Home", "End", "PageUp", "PageDown"
    *   - "Insert", "Delete"
    *
    * @param name
    *   The name of the special key
    */
  case class Special(name: String) extends Key

  /** A control character (Ctrl + key combination).
    *
    * @param char
    *   The character pressed with Ctrl
    */
  case class Control(char: Char) extends Key

  /** The Enter/Return key. */
  case object Enter extends Key

  /** The Escape key. */
  case object Escape extends Key

  /** The Backspace key. */
  case object Backspace extends Key

  /** The Tab key. */
  case object Tab extends Key
