package com.example.cart

import scala.math.BigDecimal.RoundingMode.CEILING


// --- Immutable Data Structures ---

/** Represents the base product information. Uses BigDecimal for financial precision. */
final case class Product(name: String, price: BigDecimal)

/** Represents a single product entry in the cart with its quantity. */
final case class CartItem(product: Product, quantity: Int)

/** Represents the entire shopping cart state (immutable list of items). */
final case class Cart(items: List[CartItem] = List.empty)

/** Represents the final calculated totals. */
final case class CartTotals(
                             subtotal: BigDecimal,
                             tax: BigDecimal,
                             total: BigDecimal
                           )

// --- Pure Calculation Logic ---

object CartCalculator {
  // Tax rate is 12.5%
  private val TaxRate: BigDecimal = BigDecimal("0.125")

  // All currency amounts should be scaled to two decimal places (cents)
  private val MoneyScale = 2

  // Adheres to the requirement: "Prices should be rounded up where required."
  // CEILING rounding mode rounds toward positive infinity.
  private val RoundingMode: BigDecimal.RoundingMode.Value = CEILING

  /**
   * Calculates the subtotal, tax, and total for a given cart.
   * This is a pure function (referentially transparent).
   */
  def calculateTotals(cart: Cart): CartTotals = {
    // 1. Calculate the raw subtotal (sum of price * quantity)
    val rawSubtotal = cart.items.foldLeft(BigDecimal(0)) { (acc, item) =>
      acc + (item.product.price * item.quantity)
    }

    // 2. Round the Subtotal up to two decimal places
    val subtotal = rawSubtotal.setScale(MoneyScale, RoundingMode)

    // 3. Calculate the raw tax payable (based on the rounded subtotal)
    val rawTax = subtotal * TaxRate

    // 4. Round the Tax payable up to two decimal places
    val tax = rawTax.setScale(MoneyScale, RoundingMode)

    // 5. Calculate the Total payable
    val total = subtotal + tax

    CartTotals(subtotal, tax, total)
  }

  // Helper to format the totals for easy observation
  def formatTotals(totals: CartTotals): String = {
    val fmt = "%.2f".format(_: BigDecimal)
    s"""Subtotal = ${fmt(totals.subtotal)}
       |Tax = ${fmt(totals.tax)}
       |Total = ${fmt(totals.total)}""".stripMargin
  }
}