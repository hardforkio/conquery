package com.bakdata.conquery.models.query.concept.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.filters.specific.ValidityDateSelectionFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQSelectFilter;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.bakdata.conquery.models.query.queryplan.specific.AndNode;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.queryplan.specific.SpecialDateUnionAggregatorNode;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;
import com.bakdata.conquery.models.query.select.Select;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
@CPSType(id="CONCEPT", base=CQElement.class)
public class CQConcept implements CQElement {

	private String label;
	@Valid @NotEmpty
	private List<ConceptElementId<?>> ids;
	@Valid @NotEmpty @JsonManagedReference
	private List<CQTable> tables;
	@Valid @NotNull

	private List<SelectId> select = Collections.emptyList();

	private boolean excludeFromTimeAggregation = false;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan) {
		ConceptElement[] concepts = resolveConcepts(ids, context.getCentralRegistry());
		List<Select> selects = resolveSelects(select, context.getCentralRegistry());

		List<AggregatorNode<?>> conceptAggregators = createConceptAggregators(plan, selects);

		Concept<?> c = concepts[0].getConcept();

		List<QPNode> tableNodes = new ArrayList<>();
		for(CQTable t : tables) {
			t.setResolvedConnector(c.getConnectorByName(t.getId().getConnector()));

			Select[] resolvedSelects =
					t.getSelect()
					 .stream()
					 .map(name -> context.getCentralRegistry().resolve(name.getConnector()).getSelect(name))
					 .toArray(Select[]::new);

			t.setResolvedSelects(resolvedSelects);

			List<FilterNode<?,?>> filters = new ArrayList<>(t.getFilters().size());
			//add filter to children
			for(FilterValue f : t.getFilters()) {
				FilterNode agg = f.getFilter().createAggregator(f);
				if(agg != null) {
					filters.add(agg);
				}
			}

			List<QPNode> aggregators = new ArrayList<>();
			//add aggregators

			aggregators.addAll(conceptAggregators);
			aggregators.addAll(createConceptAggregators(plan, resolveSelects(t.getSelect(), context.getCentralRegistry())));

			if(!excludeFromTimeAggregation) {
				aggregators.add(new SpecialDateUnionAggregatorNode(
					t.getResolvedConnector().getTable().getId(),
					plan.getIncluded()
				));
			}

			tableNodes.add(
				new ConceptNode(
					concepts,
					t,
					new ValidityDateNode(
						selectValidityDateColumn(t),
						conceptChild(filters, aggregators)
					)
				)
			);
		}

		return OrNode.of(tableNodes);
	}

	private List<Select> resolveSelects(List<SelectId> select, CentralRegistry centralRegistry) {
		return select.stream()
					 .map(name -> centralRegistry.resolve(name.getConnector()).<Select>getSelect(name))
					 .collect(Collectors.toList());
	}

	private ConceptElement[] resolveConcepts(List<ConceptElementId<?>> ids, CentralRegistry centralRegistry) {
		return
				ids.stream()
				   .map(id -> centralRegistry.resolve(id.findConcept()).getElementById(id))
				   .toArray(ConceptElement[]::new);
	}

	private QPNode conceptChild(List<FilterNode<?, ?>> filters, List<QPNode> aggregators) {
		QPNode result = AndNode.of(aggregators);
		if(!filters.isEmpty()) {
			result = new FiltersNode(filters, result);
		}
		return result;
	}

	private List<AggregatorNode<?>> createConceptAggregators(QueryPlan plan, List<Select> select) {

		List<AggregatorNode<?>> nodes = new ArrayList<>();

		for (Select s : select) {
			AggregatorNode<?> agg = s.createAggregator(plan.getAggregators().size());
			plan.getAggregators().add(agg.getAggregator());
			nodes.add(agg);
		}
		return nodes;
	}
	
	private Column selectValidityDateColumn(CQTable t) {
		//check if we have a manually selected validity date then use that
		for(FilterValue<?> fv : t.getFilters()) {
			if(fv instanceof CQSelectFilter && fv.getFilter() instanceof ValidityDateSelectionFilter) {
				return t
					.getResolvedConnector()
					.getValidityDateColumn(((CQSelectFilter)fv).getValue());
			}
		}
		
		//else use this first defined validity date column
		if(!t.getResolvedConnector().getValidityDates().isEmpty())
			return t.getResolvedConnector().getValidityDates().get(0).getColumn();
		else
			return null;
	}

	@Override
	public void collectSelects(Deque<Select> select) {
		select.addAll(select);
		for(CQTable table:tables) {
			select.addAll(table.getSelect());
		}
	}
}
