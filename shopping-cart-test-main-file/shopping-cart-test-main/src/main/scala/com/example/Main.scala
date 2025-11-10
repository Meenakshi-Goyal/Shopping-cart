package com.example

import cats.effect.{IOApp, IO}
import com.example.cart._
import org.http4s.ember.client.EmberClientBuilder
import com.example.cart.CartCalculator.calculateTotals

object Main extends IOApp.Simple {

  // The main run function of the application
  def run: IO[Unit] = {
    // 1. Resource management for the HTTP client (safe allocation/deallocation)
    EmberClientBuilder.default[IO].build.use { client =>
      // 2. Initialize the service layer
      val pricingService = PricingService.live[IO](client)
      val cartService = ShoppingCart[IO](pricingService)

      // 3. Define the desired cart operations (matching the sample)
      val initialCart = Cart()

      val program = for {
        // Add 2 × cornflakes
        cart1 <- cartService.addProduct(initialCart, "cornflakes", 2)
        // Add 1 × weetabix
        finalCart <- cartService.addProduct(cart1, "weetabix", 1)

        // Calculate the pure totals
        totals = calculateTotals(finalCart)

        // Print the result
        _ <- IO.println("--- Shopping Cart Demonstration ---")
        _ <- IO.println(CartCalculator.formatTotals(totals))
      } yield ()

      program
    }
  }
}