package io.github.riccardomerolla.zio.tui.example

import zio.*
import zio.test.*

object TestHelpers:

  /** Run a test with specific service implementations */
  def testWith[R, E, A](
    layer: ZLayer[Any, Nothing, R]
  )(
    test: ZIO[R, E, A]
  ): ZIO[Any, E, A] =
    test.provideLayer(layer)

  /** Create a test that expects a specific error */
  def failingTest[R, E, A](
    layer: ZLayer[Any, Nothing, R]
  )(
    test: ZIO[R, E, A]
  )(
    assertion: E => Boolean
  ): ZIO[Any, Nothing, TestResult] =
    test.provideLayer(layer).either.map {
      case Left(error) if assertion(error) => assertTrue(true)
      case Left(error) => assertTrue(false) // Wrong error
      case Right(_) => assertTrue(false) // Should have failed
    }
