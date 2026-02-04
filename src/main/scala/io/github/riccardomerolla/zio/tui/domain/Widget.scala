package io.github.riccardomerolla.zio.tui.domain

import layoutz.{ Element, stringToText }

/** A ZIO-wrapped widget ready for rendering.
  *
  * Widgets are immutable descriptions that can be composed and transformed before being rendered to the terminal.
  */
final case class Widget(element: Element):
  /** Convert this widget to a string representation for rendering. */
  def render: String = element.render

object Widget:
  /** Create a text widget from a string.
    *
    * @param text
    *   The text content to display
    * @return
    *   A Widget wrapping the text element
    */
  def text(text: String): Widget =
    Widget(layoutz.Text(text))

  /** Create a section widget with a title and content.
    *
    * @param title
    *   The section title
    * @param content
    *   The section content
    * @return
    *   A Widget wrapping the section
    */
  def section(title: String)(content: String): Widget =
    Widget(layoutz.section(title)(content))

  /** Create a list widget from multiple items.
    *
    * @param items
    *   The list items
    * @return
    *   A Widget wrapping the list
    */
  def list(items: String*): Widget =
    Widget(layoutz.ul(items.map(layoutz.Text(_))*))

  /** Create a table widget with ANSI-aware layout.
    *
    * @param headers
    *   Column headers
    * @param rows
    *   Table rows
    * @return
    *   A Widget wrapping the table
    */
  def table(headers: Seq[layoutz.Element], rows: Seq[Seq[layoutz.Element]]): Widget =
    // Render everything to strings first
    val renderedHeaders = headers.map(_.render)
    val renderedRows    = rows.map(_.map(_.render))

    // Helper to calculate visual length (stripping ANSI)
    def visualLength(s: String): Int =
      s.replaceAll("\u001b\\[[0-9;]*m", "").length

    // Calculate column widths based on visual length
    val colWidths = renderedHeaders.indices.map { i =>
      val headerLen = visualLength(renderedHeaders(i))
      val rowsLen   = if renderedRows.nonEmpty then renderedRows.map(r => visualLength(r(i))).max else 0
      math.max(headerLen, rowsLen)
    }

    // Helper to pad cell content
    def renderRow(cells: Seq[String]): String =
      cells.zipWithIndex.map {
        case (cell, i) =>
          val width   = colWidths(i)
          val visLen  = visualLength(cell)
          val padding = " " * (width - visLen)
          // Add 1 space padding around content
          s" $cell$padding "
      }.mkString("│", "│", "│")

    // Construct the table
    val topBorder    = "┌" + colWidths.map(w => "─" * (w + 2)).mkString("┬") + "┐"
    val separator    = "├" + colWidths.map(w => "─" * (w + 2)).mkString("┼") + "┤"
    val bottomBorder = "└" + colWidths.map(w => "─" * (w + 2)).mkString("┴") + "┘"

    val headerStr = renderRow(renderedHeaders)
    val rowsStr   = renderedRows.map(renderRow).mkString("\n")

    val finalTable =
      s"""$topBorder
         |$headerStr
         |$separator
         |$rowsStr
         |$bottomBorder""".stripMargin

    Widget(layoutz.Text(finalTable))

/** Result of a rendering operation.
  *
  * Contains information about the rendered output and any metadata.
  */
final case class RenderResult(
  output: String,
  lineCount: Int,
  charCount: Int,
)

object RenderResult:
  def fromString(output: String): RenderResult =
    val lines = if output.isEmpty then 0 else output.count(_ == '\n') + 1
    RenderResult(output, lines, output.length)

/** Configuration for terminal rendering.
  *
  * @param width
  *   Terminal width in characters
  * @param height
  *   Terminal height in lines
  * @param colorEnabled
  *   Whether ANSI color codes are enabled
  */
final case class TerminalConfig(
  width: Int,
  height: Int,
  colorEnabled: Boolean,
)

object TerminalConfig:
  /** Default terminal configuration with auto-detection. */
  val default: TerminalConfig =
    TerminalConfig(
      width = 80,
      height = 24,
      colorEnabled = true,
    )

/** Represents a rectangle in terminal coordinate space.
  *
  * @param x
  *   The column position (0-based, left edge)
  * @param y
  *   The row position (0-based, top edge)
  * @param width
  *   The width in columns
  * @param height
  *   The height in rows
  */
final case class Rect(
  x: Int,
  y: Int,
  width: Int,
  height: Int,
)

object Rect:
  /** Create a Rect representing full terminal size starting at origin.
    *
    * @param width
    *   Terminal width in columns
    * @param height
    *   Terminal height in rows
    * @return
    *   Rect positioned at (0,0) with given dimensions
    */
  def fromSize(width: Int, height: Int): Rect =
    Rect(x = 0, y = 0, width = width, height = height)
