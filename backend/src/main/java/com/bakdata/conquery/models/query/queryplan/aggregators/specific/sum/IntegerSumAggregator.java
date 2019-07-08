package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class IntegerSumAggregator extends SingleColumnAggregator<Long> {


	private long sum = 0;

	public IntegerSumAggregator(Column column) {
		super(column);
	}

	@Override
	public IntegerSumAggregator doClone(CloneContext ctx) {
		return new IntegerSumAggregator(getColumn());
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		long addend = bucket.getInteger(event, getColumn());

		sum += addend;
	}

	@Override
	public Long getAggregationResult() {
		return sum;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}