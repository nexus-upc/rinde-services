package com.nexus.rinde.subscription.domain.services;

import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.queries.GetSubscriptionByTenantQuery;
import com.nexus.rinde.subscription.domain.model.queries.ListActivePlansQuery;
import com.nexus.rinde.subscription.domain.model.queries.SubscriptionDetails;
import java.util.List;
import java.util.Optional;
import com.nexus.rinde.subscription.domain.model.queries.GetCurrentSubscriptionQuery;

/** Consultas del catálogo y de suscripciones visibles por empresa. */
public interface SubscriptionQueryService {

  List<Plan> handle(ListActivePlansQuery query);

  SubscriptionDetails handle(GetSubscriptionByTenantQuery query);

  Optional<SubscriptionDetails> handle(GetCurrentSubscriptionQuery query);
}
