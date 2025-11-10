package com.example.cart

import cats.effect.Async
import cats.implicits._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.Uri
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import io.circe.Decoder
import java.math.RoundingMode.HALF_UP
import com.example.cart.models.Product // Explicitly import Product for clarity

/**
 * Defines the interface for retrieving product pricing.
 * Parameterized by F[_], isolating the side-effect of the HTTP call.
 */
trait PricingService[F[_]] {
  def getProduct(productName: String): F[Product]
}

object PricingService {

  // Base URL for the pricing data repository
  private val BaseUri = "https://raw.githubusercontent.com/mattjanks16/shopping-cart-test-data/main"

  // --- JSON Decoding & Rounding ---
  implicit val productDecoder: Decoder[Product] =
    // The Decoder signature defines two fields: "product" (String) and "price" (BigDecimal).
    // The lambda parameters below MUST be used for type checking.
    Decoder.forProduct2[String, BigDecimal, Product]("product", "price") { (name, rawPrice) =>
      // The variable 'rawPrice' is correctly typed as BigDecimal here.
      val roundedPrice = rawPrice.setScale(2, HALF_UP)
      Product(name, roundedPrice)
    }

  // http4s EntityDecoder to parse the response body into the Product case class
  implicit def entityDecoder[F[_]: Async]: EntityDecoder[F, Product] = jsonOf[F, Product]

  def live[F[_]: Async](client: Client[F]): PricingService[F] = new PricingService[F] {

    // Builds the URI for a given product name (e.g., .../cheerios.json)
    private def buildUri(productName: String): F[Uri] =
      Async[F].fromEither(Uri.fromString(s"$BaseUri/${productName.toLowerCase}.json"))

    override def getProduct(productName: String): F[Product] =
      for {
        // Build the URI safely within the effect
        uri <- buildUri(productName)

        // Perform the HTTP GET request and decode the response
        product <- client.expect[Product](uri)
      } yield product
  }
}