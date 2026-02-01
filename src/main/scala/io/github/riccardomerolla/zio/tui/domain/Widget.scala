package io.github.riccardomerolla.zio.tui.domain

import layoutz.{Element, Layout as LayoutzLayout, stringToText}

/** A ZIO-wrapped widget ready for rendering.
  *
  * Widgets are immutable descriptions that can be composed and transformed
  * before being rendered to the terminal.
  */
final case class Widget(element: Element):
  /** Convert this widget to a string representation for rendering. */
  def render: String = element.render

object Widget:
  /** Create a text widget from a string.
    * 
    * @param text The text content to display
    * @return A Widget wrapping the text element
    */
  def text(text: String): Widget =
    Widget(layoutz.Text(text))
  
  /** Create a section widget with a title and content.
    * 
    * @param title The section title
    * @param content The section content
    * @return A Widget wrapping the section
    */
  def section(title: String)(content: String): Widget =
    Widget(layoutz.section(title)(content))
  
  /** Create a list widget from multiple items.
    * 
    * @param items The list items
    * @return A Widget wrapping the list
    */
  def list(items: String*): Widget =
    Widget(layoutz.ul(items.map(layoutz.Text(_))*))
  
  /** Create a table widget.
    * 
    * @param headers Column headers
    * @param rows Table rows
    * @return A Widget wrapping the table
    */
  def table(headers: Seq[String], rows: Seq[Seq[String]]): Widget =
    Widget(layoutz.table(
      headers.map(layoutz.Text(_)),
      rows.map(row => row.map(layoutz.Text(_)))
    ))

/** Result of a rendering operation.
  *
  * Contains information about the rendered output and any metadata.
  */
final case class RenderResult(
  output: String,
  lineCount: Int,
  charCount: Int
)

object RenderResult:
  def fromString(output: String): RenderResult =
    val lines = if (output.isEmpty) 0 else output.count(_ == '\n') + 1
    RenderResult(output, lines, output.length)

/** Configuration for terminal rendering.
  * 
  * @param width Terminal width in characters
  * @param height Terminal height in lines
  * @param colorEnabled Whether ANSI color codes are enabled
  */
final case class TerminalConfig(
  width: Int,
  height: Int,
  colorEnabled: Boolean
)

object TerminalConfig:
  /** Default terminal configuration with auto-detection. */
  val default: TerminalConfig =
    TerminalConfig(
      width = 80,
      height = 24,
      colorEnabled = true
    )
