package io.github.riccardomerolla.zio.tui.example

import scala.io.AnsiColor.*

import zio.*
import zio.http.Client
import zio.test.*

import io.github.riccardomerolla.zio.tui.*
import io.github.riccardomerolla.zio.tui.example.StockTickerApp.*
import io.github.riccardomerolla.zio.tui.http.domain.*

object StockTickerAppSpec extends ZIOSpecDefault:

  def spec: Spec[Environment & (TestEnvironment & Scope), Any] = suite("StockTickerAppSpec")(
    test("initial state is loading and empty") {
      val app = new TickerApp
      for
        (state, _) <- app.init.provide(HttpService.test(), Client.default)
      yield assertTrue(state.loading) &&
      assertTrue(state.stocks.isEmpty)
    },
    test("update prices correctly sets new prices and trend") {
      val app          = new TickerApp
      val initialState = StockState(Map.empty, loading = true)
      val prices1      = Map("BTCUSDT" -> 50000.0, "ETHUSDT" -> 3000.0)
      val prices2      = Map("BTCUSDT" -> 51000.0, "ETHUSDT" -> 2900.0)

      for
        // First update
        (state1, _) <- app.update(StockMsg.UpdatePrices(prices1), initialState)
                         .provide(HttpService.test(), Client.default)
        // Second update
        (state2, _) <- app.update(StockMsg.UpdatePrices(prices2), state1)
                         .provide(HttpService.test(), Client.default)
      yield
        // Check first update
        assertTrue(!state1.loading) &&
        assertTrue(state1.stocks("BTCUSDT").price == 50000.0) &&
        assertTrue(state1.stocks("BTCUSDT").prevPrice.isEmpty) &&
        // Check second update
        assertTrue(state2.stocks("BTCUSDT").price == 51000.0) &&
        assertTrue(state2.stocks("BTCUSDT").prevPrice.contains(50000.0)) &&
        assertTrue(
          state2.stocks("BTCUSDT").trend.contains(GREEN) && state2.stocks("BTCUSDT").trend.contains("▲")
        ) && // Green up
        assertTrue(state2.stocks("ETHUSDT").price == 2900.0) &&
        assertTrue(state2.stocks("ETHUSDT").prevPrice.contains(3000.0)) &&
        assertTrue(
          state2.stocks("ETHUSDT").trend.contains(RED) && state2.stocks("ETHUSDT").trend.contains("▼")
        ) // Red down
    },
    test("handles fetch errors gracefully") {
      val app          = new TickerApp
      val initialState = StockState(Map.empty, loading = true)
      for
        (state, _) <- app.update(StockMsg.FetchError("Network Timeout"), initialState)
                        .provide(HttpService.test(), Client.default)
      yield assertTrue(!state.loading) &&
      assertTrue(state.error.contains("Network Timeout"))
    },
  )
