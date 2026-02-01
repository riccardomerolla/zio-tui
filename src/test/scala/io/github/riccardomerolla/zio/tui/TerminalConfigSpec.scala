package io.github.riccardomerolla.zio.tui

import zio.test.*

import io.github.riccardomerolla.zio.tui.domain.TerminalConfig

/** ZIO Test specification for TerminalConfig.
  *
  * Tests cover:
  *   - Config creation with custom values
  *   - Default configuration
  *   - Config parameter validation
  */
object TerminalConfigSpec extends ZIOSpecDefault:

  def spec: Spec[TestEnvironment, Any] = suite("TerminalConfig")(
    suite("creation")(
      test("can be created with custom dimensions") {
        val config = TerminalConfig(width = 120, height = 40, colorEnabled = true)
        assertTrue(
          config.width == 120,
          config.height == 40,
          config.colorEnabled == true,
        )
      },
      test("can be created with colors disabled") {
        val config = TerminalConfig(width = 80, height = 24, colorEnabled = false)
        assertTrue(
          config.width == 80,
          config.height == 24,
          config.colorEnabled == false,
        )
      },
      test("stores all parameters") {
        val config = TerminalConfig(width = 100, height = 30, colorEnabled = true)
        assertTrue(
          config.width == 100,
          config.height == 30,
          config.colorEnabled,
        )
      },
    ),
    suite("default")(
      test("has standard 80x24 dimensions") {
        val config = TerminalConfig.default
        assertTrue(
          config.width == 80,
          config.height == 24,
        )
      },
      test("has colors enabled by default") {
        val config = TerminalConfig.default
        assertTrue(config.colorEnabled)
      },
    ),
    suite("equality")(
      test("configs with same values are equal") {
        val config1 = TerminalConfig(100, 50, true)
        val config2 = TerminalConfig(100, 50, true)
        assertTrue(config1 == config2)
      },
      test("configs with different values are not equal") {
        val config1 = TerminalConfig(80, 24, true)
        val config2 = TerminalConfig(100, 30, true)
        assertTrue(config1 != config2)
      },
      test("configs with different color settings are not equal") {
        val config1 = TerminalConfig(80, 24, true)
        val config2 = TerminalConfig(80, 24, false)
        assertTrue(config1 != config2)
      },
    ),
    suite("edge cases")(
      test("can create config with small dimensions") {
        val config = TerminalConfig(width = 10, height = 5, colorEnabled = true)
        assertTrue(
          config.width == 10,
          config.height == 5,
        )
      },
      test("can create config with large dimensions") {
        val config = TerminalConfig(width = 300, height = 100, colorEnabled = true)
        assertTrue(
          config.width == 300,
          config.height == 100,
        )
      },
      test("can create config with zero dimensions") {
        val config = TerminalConfig(width = 0, height = 0, colorEnabled = true)
        assertTrue(
          config.width == 0,
          config.height == 0,
        )
      },
    ),
  )
