package io.github.riccardomerolla.zio.tui

import zio.stream.ZStream
import zio.test.*
import zio.{ Scope, ZIO }

import layoutz.Element

/** ZIO Test specification for package object exports.
  *
  * Tests ensure all domain types and services are properly exported at the package level.
  */
object PackageSpec extends ZIOSpecDefault:

  def spec: Spec[TestEnvironment, Any] = suite("Package exports")(
    test("Widget type is exported") {
      val widget: Widget = Widget.text("test")
      assertTrue(widget.render.contains("test"))
    },
    test("Rect type is exported") {
      val rect: Rect = Rect.fromSize(80, 24)
      assertTrue(
        rect.width == 80,
        rect.height == 24,
      )
    },
    test("TerminalConfig type is exported") {
      val config: TerminalConfig = TerminalConfig.default
      assertTrue(
        config.width == 80,
        config.height == 24,
      )
    },
    test("RenderResult type is exported") {
      val result: RenderResult = RenderResult.fromString("test")
      assertTrue(result.output == "test")
    },
    test("ZCmd type is exported") {
      val cmd: ZCmd[Any, Nothing, String] = ZCmd.none
      assertTrue(cmd == ZCmd.None)
    },
    test("ZTuiApp type is exported") {
      val app: ZTuiApp[Any, Nothing, Int, String] = new ZTuiApp[Any, Nothing, Int, String]:
        def init: ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, String])]                            = ZIO.succeed((0, ZCmd.none))
        def update(msg: String, state: Int): ZIO[Any, Nothing, (Int, ZCmd[Any, Nothing, String])] =
          ZIO.succeed((state, ZCmd.none))
        def subscriptions(state: Int): ZStream[Any, Nothing, String]                              = ZStream.empty
        def view(state: Int): Element                                                             = layoutz.Text(state.toString)
        def run(
          clearOnStart: Boolean = true,
          clearOnExit: Boolean = true,
          showQuitMessage: Boolean = false,
          alignment: layoutz.Alignment = layoutz.Alignment.Left,
        ): ZIO[Any & Scope, Nothing, Unit] = ZIO.unit
      assertTrue(app.view(42) == layoutz.Text("42"))
    },
    test("Widget companion object is exported") {
      val widget = Widget.text("test")
      assertTrue(widget.render.contains("test"))
    },
    test("Rect companion object is exported") {
      val rect = Rect.fromSize(100, 50)
      assertTrue(
        rect.x == 0,
        rect.y == 0,
      )
    },
    test("TerminalConfig companion object is exported") {
      val config = TerminalConfig.default
      assertTrue(config.colorEnabled)
    },
    test("RenderResult companion object is exported") {
      val result = RenderResult.fromString("output")
      assertTrue(result.charCount == 6)
    },
    test("ZCmd companion object is exported") {
      val none = ZCmd.none
      val exit = ZCmd.exit
      assertTrue(none != exit)
    },
  )
