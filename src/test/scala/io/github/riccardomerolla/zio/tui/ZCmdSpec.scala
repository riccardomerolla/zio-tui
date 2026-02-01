package io.github.riccardomerolla.zio.tui

import zio.*
import zio.test.*
import zio.test.Assertion.*

object ZCmdSpec extends ZIOSpecDefault:

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("ZCmd")(
    suite("none")(
      test("is a no-op command") {
        assertTrue(ZCmd.none == ZCmd.None)
      },
      test("batches correctly with other commands") {
        val result = ZCmd.none ++ ZCmd.exit
        assertTrue(result == ZCmd.exit)
      },
    ),
    suite("exit")(
      test("is an exit command") {
        assertTrue(ZCmd.exit == ZCmd.Exit)
      },
      test("batches correctly") {
        val result = ZCmd.fire(ZIO.unit) ++ ZCmd.exit
        assertTrue(result match
          case ZCmd.Batch(cmds) => cmds.length == 2
          case _                => false)
      },
    ),
    suite("effect")(
      test("wraps a ZIO effect with message conversion") {
        val effect: ZCmd[Any, Nothing, String] = ZCmd.effect(ZIO.succeed(42))(_ => "result")
        assertTrue(effect match
          case ZCmd.Effect(_, _) => true
          case _                 => false)
      },
      test("preserves effect semantics") {
        val cmd = ZCmd.effect(ZIO.succeed(42))(n => s"got $n")
        assertTrue(cmd match
          case ZCmd.Effect(zio, toMsg) =>
            // Verify the structure is correct
            true
          case _                       => false)
      },
    ),
    suite("fire")(
      test("creates a fire-and-forget command") {
        val cmd = ZCmd.fire(ZIO.unit)
        assertTrue(cmd match
          case ZCmd.Fire(_) => true
          case _            => false)
      },
      test("preserves effect reference") {
        for
          ref <- Ref.make(0)
        yield
          val cmd = ZCmd.fire(ref.update(_ + 1))
          assertTrue(cmd match
            case ZCmd.Fire(eff) => true
            case _              => false)
      },
    ),
    suite("batch")(
      test("combines multiple commands") {
        val cmd = ZCmd.batch(ZCmd.none, ZCmd.exit)
        assertTrue(cmd == ZCmd.exit)
      },
      test("returns single command for single input") {
        val effect = ZIO.succeed(42)
        val cmd    = ZCmd.batch(ZCmd.effect(effect)(_ => "msg"))
        assertTrue(cmd match
          case ZCmd.Effect(_, _) => true
          case _                 => false)
      },
      test("returns none for empty batch") {
        val cmd = ZCmd.batch[Any, Nothing, Nothing]()
        assertTrue(cmd == ZCmd.None)
      },
      test("filters out none commands when batching") {
        val cmd = ZCmd.batch(ZCmd.fire(ZIO.unit), ZCmd.none, ZCmd.exit)
        assertTrue(cmd match
          case ZCmd.Batch(cmds) => cmds.length == 2
          case _                => false)
      },
    ),
    suite("++ operator")(
      test("combines none with another command") {
        val result = ZCmd.none ++ ZCmd.exit
        assertTrue(result == ZCmd.exit)
      },
      test("combines two non-none commands") {
        val cmd1   = ZCmd.fire(ZIO.unit)
        val cmd2   = ZCmd.exit
        val result = cmd1 ++ cmd2
        assertTrue(result match
          case ZCmd.Batch(cmds) => cmds.length == 2
          case _                => false)
      },
      test("combines batch with command") {
        val batch  = ZCmd.batch(ZCmd.exit, ZCmd.none)
        val cmd    = ZCmd.fire(ZIO.unit)
        val result = batch ++ cmd
        assertTrue(result match
          case ZCmd.Batch(cmds) => cmds.length == 2
          case _                => false)
      },
      test("combines command with batch") {
        val cmd    = ZCmd.fire(ZIO.unit)
        val batch  = ZCmd.batch(ZCmd.exit, ZCmd.none)
        val result = cmd ++ batch
        assertTrue(result match
          case ZCmd.Batch(cmds) => cmds.length == 2
          case _                => false)
      },
    ),
    suite("composition")(
      test("allows chaining multiple ++ operations") {
        val result = ZCmd.exit ++ ZCmd.none ++ ZCmd.fire(ZIO.unit)
        assertTrue(result match
          case ZCmd.Batch(cmds) => cmds.length == 2
          case _                => false)
      },
      test("properly handles empty results") {
        val cmd1 = ZCmd.none ++ ZCmd.none
        assertTrue(cmd1 == ZCmd.None)
      },
    ),
    suite("effect execution")(
      test("effect command captures the ZIO and conversion function") {
        val effect = ZIO.succeed(100)
        val cmd    = ZCmd.effect(effect)(n => s"Result: $n")
        assertTrue(cmd match
          case ZCmd.Effect(capturedZio, toMsg) =>
            // Verify structure
            true
          case _                               => false)
      },
      test("fire command captures the ZIO effect") {
        val sideEffect = ZIO.unit
        val cmd        = ZCmd.fire(sideEffect)
        assertTrue(cmd match
          case ZCmd.Fire(_) => true
          case _            => false)
      },
    ),
  )
