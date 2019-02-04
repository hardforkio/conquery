package com.bakdata.conquery.models.query.concept.specific.temporal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.BeforeTemporalPrecedenceMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalQueryNode;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened before the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "BEFORE", base = CQElement.class)
public class CQBeforeTemporalQuery extends CQAbstractTemporalQuery {


	public CQBeforeTemporalQuery(CQElement index, CQElement preceding, TemporalSampler sampler) {
		super(index, preceding, sampler);
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext registry, QueryPlan plan) {

		QueryPlan indexPlan = QueryPlan.create();
		indexPlan.setRoot(index.createQueryPlan(registry, plan));

		QueryPlan precedingPlan = QueryPlan.create();
		precedingPlan.setRoot(preceding.createQueryPlan(registry, plan));

		return new TemporalQueryNode(indexPlan, precedingPlan, getSampler(), new BeforeTemporalPrecedenceMatcher(), plan.getIncluded());
	}
	
	@Override
	public CQBeforeTemporalQuery resolve(QueryResolveContext context) {
		return new CQBeforeTemporalQuery(index.resolve(context), preceding.resolve(context), sampler);
	}
}