/*
 * Copyright (C) 2016-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.scaladsl.persistence

import akka.cluster.sharding.typed.scaladsl.EntityContext

object AkkaTaggerAdapter {

  /**
   * Adapts an existing Lagom [[AggregateEventTagger]] to a
   * function {{{Event => Set[String]}}} as expected by Akka Persistence Typed {{{EventSourcedBehavior.withTagger}}} API.
   */
  def fromLagom[Event <: AggregateEvent[Event]](
      entityCtx: EntityContext,
      lagomTagger: AggregateEventTagger[Event]
  ): Event => Set[String] = { evt =>
    val tag =
      lagomTagger match {
        case tagger: AggregateEventTag[_] =>
          tagger.tag
        case shardedTagger: AggregateEventShards[_] =>
          shardedTagger.forEntityId(entityCtx.entityId).tag
      }
    Set(tag)
  }
}
