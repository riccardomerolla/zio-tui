package io.github.riccardomerolla.zio.tui.error

/** Typed errors for terminal UI operations.
  *
  * All errors in zio-tui are explicitly typed and never thrown as exceptions.
  * This enables exhaustive pattern matching and compile-time guarantees.
  */
enum TUIError:
  /** Terminal initialization failed.
    * 
    * @param reason Description of what caused the initialization failure
    */
  case InitializationFailed(reason: String)
  
  /** Rendering operation failed.
    * 
    * @param widget The widget that failed to render
    * @param cause The underlying reason for the failure
    */
  case RenderingFailed(widget: String, cause: String)
  
  /** Terminal resource was accessed after being closed.
    */
  case TerminalClosed
  
  /** Invalid terminal dimensions or size.
    * 
    * @param width The requested or current width
    * @param height The requested or current height
    * @param reason Description of why the dimensions are invalid
    */
  case InvalidDimensions(width: Int, height: Int, reason: String)
  
  /** IO operation on the terminal failed.
    * 
    * @param operation Description of the operation
    * @param cause The underlying exception or reason
    */
  case IOError(operation: String, cause: String)
