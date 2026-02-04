package io.github.riccardomerolla.zio.tui.example

import scala.io.AnsiColor.*

import zio.*
import zio.http.Client
import zio.stream.ZStream

import io.github.riccardomerolla.zio.tui.*
import io.github.riccardomerolla.zio.tui.domain.*
import io.github.riccardomerolla.zio.tui.subscriptions.*
import layoutz.{ Element, stringToText }

/** Stock Ticker application demonstrating ZIO HTTP integration and streaming.
  *
  * Features:
  *   - Real-time price updates using Binance API
  *   - Multi-symbol tracking in a table
  *   - Color-coded price changes (green for up, red for down)
  *   - Error handling and loading states
  *   - Periodic polling with ZIO Schedule
  */
object StockTickerApp extends ZIOAppDefault:

  case class StockData(
    symbol: String,
    price: Double,
    prevPrice: Option[Double] = None,
  ):
    def trend: String =
      prevPrice match
        case Some(prev) if price > prev => s"${GREEN}▲${RESET}"
        case Some(prev) if price < prev => s"${RED}▼${RESET}"
        case _                          => " "

    def formattedPrice: String =
      val color = prevPrice match
        case Some(prev) if price > prev => GREEN
        case Some(prev) if price < prev => RED
        case _                          => RESET
      s"$color${f"$price%1.4f"}$RESET"

  case class StockState(
    stocks: Map[String, StockData],
    loading: Boolean = true,
    error: Option[String] = None,
  )

  enum StockMsg:
    case UpdatePrices(prices: Map[String, Double])
    case FetchError(error: String)
    case Quit

  type AppEnv = HttpService & Client

  class TickerApp extends ZTuiApp[AppEnv, HttpError, StockState, StockMsg]:
    private val symbols       = List("BTCUSDT", "ETHUSDT", "SOLUSDT", "ADAUSDT", "DOTUSDT")
    private val apiUrl        = "https://api.binance.com/api/v3/ticker/price"
    private val symbolRegexes =
      symbols.map(sym => sym -> s""""symbol":"$sym","price":"([0-9.]+)"""".r).toMap

    def init: ZIO[AppEnv, HttpError, (StockState, ZCmd[AppEnv, HttpError, StockMsg])] =
      ZIO.succeed((StockState(Map.empty), ZCmd.none))

    def update(
      msg: StockMsg,
      state: StockState,
    ): ZIO[AppEnv, HttpError, (StockState, ZCmd[AppEnv, HttpError, StockMsg])] =
      msg match
        case StockMsg.UpdatePrices(newPrices) =>
          val updatedStocks = symbols.map { sym =>
            val currentPrice = newPrices.getOrElse(sym, 0.0)
            val oldData      = state.stocks.get(sym)
            sym -> StockData(sym, currentPrice, oldData.map(_.price))
          }.toMap
          ZIO.succeed((state.copy(stocks = updatedStocks, loading = false, error = None), ZCmd.none))

        case StockMsg.FetchError(err) =>
          ZIO.succeed((state.copy(error = Some(err), loading = false), ZCmd.none))

        case StockMsg.Quit =>
          ZIO.succeed((state, ZCmd.exit))

    def subscriptions(state: StockState): ZStream[AppEnv, HttpError, StockMsg] =
      val pollStream = HttpService
        .poll(apiUrl, Schedule.spaced(5.seconds)) { response =>
          val prices = symbolRegexes.flatMap {
            case (sym, regex) =>
              regex
                .findFirstMatchIn(response.body)
                .flatMap(m => m.group(1).toDoubleOption.map(sym -> _))
          }
          StockMsg.UpdatePrices(prices)
        }
        .catchAll(err => ZStream.succeed(StockMsg.FetchError(err.toString)))

      val keyStream = ZSub
        .keyPress {
          case Key.Character('q') | Key.Escape => Some(StockMsg.Quit)
          case _                               => None
        }
        .mapError(_ => HttpError.NetworkError("Keyboard subscription failed"))

      pollStream.merge(keyStream)

    def view(state: StockState): Element =
      if state.loading && state.stocks.isEmpty then layoutz.section("Stock Ticker")("Loading market data...")
      else
        val errorSection = state.error.map(err => s"${RED}Error: $err${RESET}\n").getOrElse("")
        val header       = List("Symbol", "Price", "Trend").map(layoutz.Text(_))
        val rows         = symbols.flatMap(s => state.stocks.get(s)).map { stock =>
          List(
            layoutz.Text(stock.symbol),
            layoutz.Text(stock.formattedPrice),
            layoutz.Text(stock.trend),
          )
        }

        layoutz.layout(
          layoutz.section("ZIO-TUI Crypto Ticker")(
            errorSection + "Real-time data from Binance API (5s refresh)\nPress 'q' to quit"
          ),
          Widget.table(header, rows).element,
        )

    def run(
      clearOnStart: Boolean = true,
      clearOnExit: Boolean = true,
      showQuitMessage: Boolean = false,
      alignment: layoutz.Alignment = layoutz.Alignment.Left,
    ): ZIO[AppEnv & Scope, HttpError, Unit] =
      ZIO.unit

  def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    val clearScreen: ZIO[Any, Nothing, Unit] =
      Console.print("\u001b[2J\u001b[H").orDie

    def renderView(state: StockState, app: TickerApp): ZIO[Any, Nothing, Unit] =
      for
        _ <- Console.print("\u001b[H").orDie
        _ <- Console.printLine(app.view(state).render).orDie
      yield ()

    val appLayer: ZLayer[Any, Throwable, AppEnv] =
      Client.default >>> (HttpService.liveWithClient ++ ZLayer.service[Client])

    (for
      _                 <- Console.printLine("Starting Stock Ticker...").orDie
      app               <- ZIO.succeed(new TickerApp)
      (initialState, _) <- app.init
      _                 <- clearScreen
      _                 <- renderView(initialState, app)
      stateRef          <- Ref.make(initialState)
      _                 <- app
                             .subscriptions(initialState)
                             .takeUntil(_ == StockMsg.Quit)
                             .foreach { msg =>
                               msg match
                                 case StockMsg.Quit =>
                                   ZIO.unit
                                 case _             =>
                                   for
                                     currentState  <- stateRef.get
                                     (newState, _) <- app.update(msg, currentState)
                                     _             <- stateRef.set(newState)
                                     _             <- renderView(newState, app)
                                   yield ()
                             }
                             .catchAllCause(cause => ZIO.logErrorCause("Stock ticker stream failed", cause))
      _                 <- clearScreen
      _                 <- Console.printLine("Ticker stopped.").orDie
    yield ())
      .provideLayer(appLayer)
