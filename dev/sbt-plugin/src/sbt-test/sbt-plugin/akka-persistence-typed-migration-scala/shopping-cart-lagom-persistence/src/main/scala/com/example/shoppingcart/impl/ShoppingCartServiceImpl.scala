/*
 * Copyright (C) 2016-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.shoppingcart.impl

import java.time.OffsetDateTime

import akka.{Done, NotUsed}
import com.example.shoppingcart.api.ShoppingCartService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound, TransportException}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

import scala.concurrent.ExecutionContext

/**
  * Implementation of the `ShoppingCartService`.
  */
class ShoppingCartServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, reportRepository: ShoppingCartReportRepository)(implicit ec: ExecutionContext) extends ShoppingCartService {

  /**
    * Looks up the shopping cart entity for the given ID.
    */
  private def entityRef(id: String) =
    persistentEntityRegistry.refFor[ShoppingCartEntity](id)

  override def get(id: String): ServiceCall[NotUsed, String] = ServiceCall { _ =>
    entityRef(id)
      .ask(Get)
      .map(cart => convertShoppingCart(id, cart))
  }

  override def updateItem(id: String, productId: String, qty: Int): ServiceCall[NotUsed, Done] = ServiceCall { update =>
    entityRef(id)
      .ask(UpdateItem(productId, qty))
      .recover {
        case ShoppingCartException(message) => throw BadRequest(message)
      }
  }

  override def checkout(id: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    entityRef(id)
      .ask(Checkout)
      .recover {
        case ShoppingCartException(message) => throw BadRequest(message)
      }
  }

  private def convertShoppingCart(id: String, cart: ShoppingCartState): String = {
    val items = cart.items.map {case (k, v) => s"$k=$v"}.mkString(":")
    val status = if (cart.checkedOut) "checkedout" else "open"
    s"$id:$items:$status"
  }

  override def getReport(cartId: String): ServiceCall[NotUsed, String] = ServiceCall { _ =>
    reportRepository.findById(cartId).map {
      case Some(cart) =>
        if (cart.checkedOut) "checkedout"
        else "open"
      case None => throw NotFound(s"Couldn't find a shopping cart report for '$cartId'")
    }
  }
}
