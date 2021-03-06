package com.bakdata.conquery.models.query.queryplan;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;

public interface EventIterating {
	
	default void collectRequiredTables(Set<TableId> requiredTables) {}
	
	default Set<TableId> collectRequiredTables() {
		HashSet<TableId> out = new HashSet<>();
		this.collectRequiredTables(out);
		return out;
	}
	
	default void nextTable(QueryExecutionContext ctx, Table currentTable) {}
	
	default void nextBlock(Bucket bucket) {}
	
	boolean isOfInterest(Bucket bucket);
	
	boolean isOfInterest(Entity entity);
}
