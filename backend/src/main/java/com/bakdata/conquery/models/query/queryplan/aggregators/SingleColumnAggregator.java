package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Set;


@AllArgsConstructor
public abstract class SingleColumnAggregator<T> extends ColumnAggregator<T> {

	@Valid
	@NotNull
	@Getter
	@Setter
	@NsIdRef
	protected Column column;

	@Override
	public Column[] getRequiredColumns() {
		return new Column[] { getColumn() };
	}

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		out.add(getColumn().getTable().getId());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{column};
	}

	@Override
	public abstract SingleColumnAggregator<T> clone();
}
