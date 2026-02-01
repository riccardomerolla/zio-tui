package io.github.riccardomerolla.zio.tui

import zio.Scope
import zio.test.{ Spec, TestEnvironment, ZIOSpecDefault, assertTrue }

import io.github.riccardomerolla.zio.tui.domain.{ RenderResult, Widget }

/** ZIO Test specification for Widget domain models.
  *
  * Tests cover:
  *   - Widget creation from various sources
  *   - Widget composition
  *   - Render result calculation
  */
object WidgetSpec extends ZIOSpecDefault:

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("Widget")(
    suite("text widget")(
      test("creates from string") {
        val widget = Widget.text("Hello")
        assertTrue(widget.render.contains("Hello"))
      },
      test("handles empty string") {
        val widget = Widget.text("")
        assertTrue(widget.render.isEmpty)
      },
      test("handles multiline text") {
        val widget   = Widget.text("Line 1\nLine 2")
        val rendered = widget.render
        assertTrue(
          rendered.contains("Line 1"),
          rendered.contains("Line 2"),
        )
      },
    ),
    suite("section widget")(
      test("creates section with title and content") {
        val widget   = Widget.section("Title")("Content")
        val rendered = widget.render
        assertTrue(
          rendered.contains("Title"),
          rendered.contains("Content"),
        )
      }
    ),
    suite("list widget")(
      test("creates list from varargs") {
        val widget   = Widget.list("A", "B", "C")
        val rendered = widget.render
        assertTrue(
          rendered.contains("A"),
          rendered.contains("B"),
          rendered.contains("C"),
        )
      },
      test("handles empty list") {
        val widget = Widget.list()
        // Empty list renders to empty string in layoutz
        assertTrue(widget.render.isEmpty || widget.render.trim.isEmpty)
      },
    ),
    suite("table widget")(
      test("creates table with headers and rows") {
        val widget   = Widget.table(
          Seq("Col1", "Col2"),
          Seq(
            Seq("A", "B"),
            Seq("C", "D"),
          ),
        )
        val rendered = widget.render
        assertTrue(
          rendered.contains("Col1"),
          rendered.contains("Col2"),
          rendered.contains("A"),
          rendered.contains("B"),
          rendered.contains("C"),
          rendered.contains("D"),
        )
      }
    ),
    suite("RenderResult")(
      test("calculates line count correctly") {
        val result = RenderResult.fromString("Line 1\nLine 2\nLine 3")
        assertTrue(result.lineCount == 3)
      },
      test("calculates char count correctly") {
        val text   = "Hello, World!"
        val result = RenderResult.fromString(text)
        assertTrue(result.charCount == text.length)
      },
      test("handles single line") {
        val result = RenderResult.fromString("Single line")
        assertTrue(result.lineCount == 1)
      },
      test("handles empty string") {
        val result = RenderResult.fromString("")
        assertTrue(
          result.lineCount == 0,
          result.charCount == 0,
        )
      },
    ),
  )
