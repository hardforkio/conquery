package com.bakdata.conquery.models.forms.managed;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@CPSType(id="RELATIVE_FORM_QUERY", base=QueryDescription.class)
@Getter
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class RelativeFormQuery extends IQuery {
	@NotNull @Valid
	private final IQuery query;
	@NotNull @Valid
	private final ArrayConceptQuery features;
	@NotNull @Valid
	private final ArrayConceptQuery outcomes;
	@NotNull
	private final TemporalSampler indexSelector;
	@NotNull
	private final IndexPlacement indexPlacement;
	@Min(0)
	private final int timeCountBefore;
	@Min(0)
	private final int timeCountAfter;
	@NotNull
	private final DateContextMode timeUnit;
	@NotNull
	private final List<DateContextMode> resolutions;
	
	@Override
	public RelativeFormQuery resolve(QueryResolveContext context) {
		return new RelativeFormQuery(
			query.resolve(context),
			features.resolve(context),
			outcomes.resolve(context),
			indexSelector,
			indexPlacement,
			timeCountBefore,
			timeCountAfter,
			timeUnit,
			resolutions
		);
	}
	
	@Override
	public RelativeFormQueryPlan createQueryPlan(QueryPlanContext context) {
		return new RelativeFormQueryPlan(query.createQueryPlan(context.withGenerateSpecialDateUnion(true)),
			// At the moment we do not use the dates of feature and outcome query
			features.createQueryPlan(context.withGenerateSpecialDateUnion(false)),
			outcomes.createQueryPlan(context.withGenerateSpecialDateUnion(false)),
			indexSelector, indexPlacement, timeCountBefore,	timeCountAfter, timeUnit, resolutions);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
		features.collectRequiredQueries(requiredQueries);
		outcomes.collectRequiredQueries(requiredQueries);
	}
	
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		ResultInfoCollector featureHeader = features.collectResultInfos();
		ResultInfoCollector outcomeHeader = outcomes.collectResultInfos();
		//remove SpecialDateUnion
		featureHeader.getInfos().remove(0);
		outcomeHeader.getInfos().remove(0);
		//remove resolution info
		featureHeader.getInfos().remove(ConqueryConstants.RESOLUTION_INFO);
		outcomeHeader.getInfos().remove(ConqueryConstants.RESOLUTION_INFO);

		// resolution
		collector.add(ConqueryConstants.RESOLUTION_INFO);
		// index
		collector.add(ConqueryConstants.CONTEXT_INDEX_INFO);
		// event date
		collector.add(ConqueryConstants.EVENT_DATE_INFO);
		//date ranges
		collector.add(ConqueryConstants.FEATURE_DATE_RANGE_INFO);
		collector.add(ConqueryConstants.OUTCOME_DATE_RANGE_INFO);
		//features
		collector.addAll(featureHeader.getInfos());
		collector.addAll(outcomeHeader.getInfos());
	}
	
	@Override
	public void visit(Consumer<Visitable> visitor) {
		query.visit(visitor);
		outcomes.visit(visitor);
		features.visit(visitor);
	}
}
