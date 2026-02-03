package io.github.riccardomerolla.zio.tui.subscriptions

import zio.*
import zio.stream.*

import io.github.riccardomerolla.zio.tui.error.TUIError

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
    * Useful for periodic tasks like refreshing data, updating clocks, or polling external state. The stream never fails
    * and emits infinitely until interrupted.
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

  /** Merge multiple subscriptions into a single stream.
    *
    * Combines multiple subscription streams, interleaving their emissions. All input streams run concurrently, and
    * emissions from any stream appear in the merged output. The merged stream fails if any input stream fails and
    * completes when all input streams complete.
    *
    * Backpressure: Uses ZStream's built-in backpressure handling (16-element default buffer) across all merged streams.
    *
    * @param subs
    *   Variable number of streams to merge
    * @return
    *   A stream that emits values from all input streams
    *
    * @example
    *   {{{
    * ZSub.merge(
    *   ZSub.tick(1.second).map(_ => Msg.Tick),
    *   ZSub.keyPress(handler),
    *   ZSub.watchFile("config.json").map(Msg.ConfigChanged)
    * )
    *   }}}
    */
  def merge[R, E, Msg](subs: ZStream[R, E, Msg]*): ZStream[R, E, Msg] =
    if subs.isEmpty then ZStream.empty
    else ZStream.mergeAll(subs.size)(subs*)

  /** Watch a file for changes and emit its content.
    *
    * Polls the file at regular intervals (100ms) and emits the file content whenever it changes. Uses MD5 hashing to
    * efficiently detect changes without comparing full content. The stream fails if the file doesn't exist or cannot be
    * read.
    *
    * Implementation uses simple polling for cross-platform compatibility rather than OS-native file watching.
    *
    * Backpressure: Polling rate (100ms) naturally bounds production rate.
    *
    * @param path
    *   Path to the file to watch
    * @return
    *   A stream that emits file content on changes
    *
    * @example
    *   {{{
    * ZSub.watchFile("config.json")
    *   .map(content => Msg.ConfigChanged(content))
    *   .catchAll(err => ZStream.succeed(Msg.Error(err)))
    *   }}}
    */
  def watchFile(path: String): ZStream[Any, TUIError, String] =
    ZStream.unwrap {
      for
        lastHashRef <- Ref.make[Option[String]](None)
      yield ZStream
        .repeatWithSchedule(ZIO.unit, Schedule.fixed(100.millis))
        .mapZIO { _ =>
          for
            exists  <- ZIO.attempt(java.nio.file.Files.exists(java.nio.file.Paths.get(path)))
                         .mapError(e => TUIError.IOError(s"Failed to check file: $path", e.getMessage))
            _       <- ZIO.when(!exists)(ZIO.fail(TUIError.FileNotFound(path)))
            content <- ZIO.attempt(java.nio.file.Files.readString(java.nio.file.Paths.get(path)))
                         .mapError(e => TUIError.IOError(s"Failed to read file: $path", e.getMessage))
            hash    <- ZIO.succeed {
                         val md = java.security.MessageDigest.getInstance("MD5")
                         md.digest(content.getBytes).map("%02x".format(_)).mkString
                       }
            changed <- lastHashRef.modify { lastHash =>
                         val hasChanged = lastHash.forall(_ != hash)
                         (hasChanged, Some(hash))
                       }
          yield if changed then Some(content) else None
        }
        .collectSome
    }

  /** Subscribe to keyboard input events.
    *
    * Reads keyboard input from stdin using JLine3's NonBlockingReader in raw mode. The handler function maps Key events
    * to application messages, returning Some(msg) to emit a message or None to ignore the key.
    *
    * This is particularly useful for interactive TUI applications that need to respond to keyboard input. The stream
    * runs continuously until interrupted and automatically handles terminal cleanup.
    *
    * @param handler
    *   Function that maps Key events to optional messages
    * @return
    *   A stream that emits messages based on keyboard input
    *
    * @example
    *   {{{
    * def subscriptions(state: State): ZStream[Any, Nothing, Msg] =
    *   ZSub.keyPress {
    *     case Key.Character('q') => Some(Msg.Quit)
    *     case Key.Character('+') => Some(Msg.Increment)
    *     case Key.Character('-') => Some(Msg.Decrement)
    *     case _                  => None
    *   }
    *   }}}
    */
  def keyPress[Msg](handler: Key => Option[Msg]): ZStream[Any, Nothing, Msg] =
    ZStream.unwrap {
      ZIO.scoped {
        ZIO
          .acquireRelease(
            acquire = ZIO.attemptBlocking {
              val terminal = org.jline.terminal.TerminalBuilder.terminal()
              terminal.enterRawMode()
              (terminal, terminal.reader())
            }
          )(release = { case (terminal, _) =>
            ZIO.attemptBlocking(terminal.close()).ignore
          })
          .map { case (terminal, reader) =>
            ZStream
              .repeatZIO {
                ZIO.attemptBlocking {
                  val char = reader.read()
                  if char == -1 then None
                  else
                    val key = char.toChar match
                      case '\n' | '\r'         => Key.Enter
                      case '\u001b'            => Key.Escape
                      case '\u007f'            => Key.Backspace
                      case '\t'                => Key.Tab
                      case c if c.toInt < 32   => Key.Control((c.toInt + 96).toChar)
                      case c                   => Key.Character(c)
                    handler(key)
                }.catchAll(_ => ZIO.succeed(None))
              }
              .collectSome
          }
      }.catchAll(_ => ZIO.succeed(ZStream.empty))
    }
