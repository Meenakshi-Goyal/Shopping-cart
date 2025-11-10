package com.example.cart

import cats.Monad

/**
 * Defines the shopping cart management logic.
 * It uses the Monad F[_] to compose the effectful PricingService with pure data updates.
 */
trait ShoppingCart[F[_]] {
  /**
   * Adds a specified quantity of a product to the cart.
   * @param cart The current immutable cart state.
   * @param productName The name of the product to add.
   * @param quantity The number of units to add.
   * @return F[Cart] The new immutable cart state wrapped in the effect F.
   */
  def addProduct(cart: Cart, productName: String, quantity: Int): F[Cart]
}

object ShoppingCart {

  /**
   * Factory method for creating a ShoppingCart implementation.
   */
  def apply[F[_]: Monad](pricingService: PricingService[F]): ShoppingCart[F] =
    new ShoppingCart[F] {

      override def addProduct(cart: Cart, productName: String, quantity: Int): F[Cart] = {

        if (quantity <= 0) {
          // If quantity is zero or less, return the original cart state without effect
          Monad[F].pure(cart)
        } else {
          // Sequence the effect (price lookup) and the pure update logic
          for {
            // F[Product]: Effectful call to get the product price
            product <- pricingService.getProduct(productName)

            // Pure Logic: Create the new immutable Cart state
            item = CartItem(product, quantity)

            // For simplicity and to meet the "add products" requirement:
            // If the product is already in the cart, replace the entry with the new quantity.
            newItems = cart.items.filterNot(_.product.name.equalsIgnoreCase(productName)) :+ item

          } yield Cart(newItems)
        }
      }
    }
}