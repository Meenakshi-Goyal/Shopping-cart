package com.example.cart

import cats.effect.IO
import munit.CatsEffectSuite
import com.example.cart.CartCalculator.calculateTotals
import com.example.cart.PricingService

import scala.math.BigDecimal.RoundingMode.HALF_UP


/**
 * Stub implementation of PricingService for self-contained testing.
 * This is crucial for avoiding reliance on external systems (HTTP).
 * It returns prices inside the IO effect context.
 */
class StubPricingService extends PricingService[IO] {
  private val prices = Map(
    "cornflakes" -> BigDecimal("2.52").setScale(2, HALF_UP),
    "weetabix"   -> BigDecimal("9.98").setScale(2, HALF_UP),
    "cheerios"   -> BigDecimal("4.00").setScale(2, HALF_UP),
    "frosties"   -> BigDecimal("3.50").setScale(2, HALF_UP)
  )

  override def getProduct(productName: String): IO[Product] =
    prices.get(productName.toLowerCase) match {
      case Some(price) => IO.pure(Product(productName.toLowerCase, price))
      // Simulate a product not found error
      case None        => IO.raiseError(new RuntimeException(s"Product not found: $productName"))
    }
}

class ShoppingCartSuite extends CatsEffectSuite {

  // Initialize the services with the stub for self-contained, isolated testing
  private val pricingService: PricingService[IO] = new StubPricingService()
  private val cartService: ShoppingCart[IO] = ShoppingCart[IO](pricingService)

  // --- Test Case 1: Verification of Sample Data (The core requirement check) ---
  test("Cart totals calculation must match the provided sample data (Rounding Up)") {
    val initialCart = Cart()

    // Add 2 × cornflakes @ 2.52 each
    val cart1F = cartService.addProduct(initialCart, "cornflakes", 2)

    // Add 1 × weetabix @ 9.98 each
    val finalCartF = cart1F.flatMap(cart => cartService.addProduct(cart, "weetabix", 1))

    finalCartF.map(finalCart => {
      val totals = calculateTotals(finalCart)

      // Expected calculation:
      // Subtotal: (2 * 2.52) + (1 * 9.98) = 15.02
      // Tax (12.5%): 15.02 * 0.125 = 1.8775 -> Rounded up (CEILING) to 1.88
      // Total: 15.02 + 1.88 = 16.90

      assertEquals(totals.subtotal, BigDecimal("15.02"))
      assertEquals(totals.tax, BigDecimal("1.88"))
      assertEquals(totals.total, BigDecimal("16.90"))
    })
  }

  // --- Test Case 2: Pure Calculation Logic Test (No IO/Stub involved) ---
  test("Pure calculator handles complex rounding correctly") {
    // Manually construct a cart with known items, avoiding the effectful service
    val cart = Cart(List(
      CartItem(Product("A", BigDecimal("1.00")), 1),
      CartItem(Product("B", BigDecimal("1.07")), 1) // Subtotal = 2.07
    ))

    val totals = calculateTotals(cart)

    // Expected calculation:
    // Subtotal: 2.07
    // Tax (12.5%): 2.07 * 0.125 = 0.25875 -> Rounded up (CEILING) to 0.26
    // Total: 2.07 + 0.26 = 2.33

    assertEquals(totals.subtotal, BigDecimal("2.07"))
    assertEquals(totals.tax, BigDecimal("0.26"))
    assertEquals(totals.total, BigDecimal("2.33"))
  }

  // --- Test Case 3: Error Path ---
  test("addProduct must raise error if pricing service cannot find product") {
    val initialCart = Cart()
    // Attempt to add an unknown product
    val resultF = cartService.addProduct(initialCart, "unknown_item", 1).attempt

    // We assert that the result is a failure (Left)
    resultF.map(result => assert(result.isLeft))
  }

  // --- Test Case 4: Zero Quantity Addition ---
  test("Adding a zero quantity product should not change the cart state") {
    val cartF = cartService.addProduct(Cart(), "frosties", 5)

    // Attempt to add zero quantity of another product
    val finalCartF = cartF.flatMap(cart => cartService.addProduct(cart, "cheerios", 0))

    finalCartF.map(finalCart => {
      // The final cart should only contain 'frosties'
      assertEquals(finalCart.items.size, 1)
      assertEquals(finalCart.items.head.product.name, "frosties")
      assertEquals(finalCart.items.head.quantity, 5)
    })
  }

  // --- Test Case 5: Overwriting (or replacing) Quantity ---
  test("Adding an existing product should replace its quantity") {
    val cartF = for {
      c1 <- cartService.addProduct(Cart(), "cheerios", 5) // 5 units
      c2 <- cartService.addProduct(c1, "cheerios", 2)  // replace with 2 units
    } yield c2

    cartF.map(finalCart => {
      // The cart should still have only one item, with the last specified quantity (2)
      assertEquals(finalCart.items.size, 1)
      assertEquals(finalCart.items.head.product.name, "cheerios")
      assertEquals(finalCart.items.head.quantity, 2)
    })
  }
}