package com.bakdata.eva.models.forms;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DateContext {

	/**
	 * The date range.
	 *
	 * @return The date range
	 */
	@Getter
	private final CDateRange dateRange;

	/**
	 * Indicates to which group the context belongs.
	 *
	 * @return The groups.
	 */
	@Getter
	@Setter
	private FeatureGroup featureGroup;

	/**
	 * Indicates the relative position of the context to the event context.
	 *
	 * @return The index.
	 */
	@Getter
	private Integer index = null;
	
	/**
	 * The date from which the relative context were generated.
	 * 
	 * @return The event date
	 */
	@Getter @Setter
	private LocalDate eventDate = null;

	/**
	 * Returns the date ranges that fit into a mask specified as date range, which
	 * are optional subdivided in to year-wise or quarter-wise date ranges.
	 *
	 * @param dateRangeMask The mask that is applied onto the dates.
	 * @param resultMode    The subdivision mode that defines the granularity of the
	 *                      result.
	 * @return All date ranges as wrapped into {@link DateContext} that were in the
	 *         mask.
	 */
	public static List<DateContext> generateAbsoluteContexts(CDateRange dateRangeMask, DateContextMode resultMode) {
		List<DateContext> dcList = new ArrayList<>();

		// Add whole time span
		DateContext dc = new DateContext(dateRangeMask);
		dc.setFeatureGroup(FeatureGroup.OUTCOME);
		dcList.add(dc);

		int index = 0;
		// Handle years
		if (resultMode == DateContextMode.YEAR_WISE) {
			List<CDateRange> maskYears = dateRangeMask.getCoveredYears();
			for (CDateRange yearInMask : maskYears) {
				dc = new DateContext(
					yearInMask,
					FeatureGroup.OUTCOME,
					index++,
					null
				);
				dcList.add(dc);
			}
		}

		// Handle quarters
		if (resultMode == DateContextMode.QUARTER_WISE) {
			List<CDateRange> maskQuarters = dateRangeMask.getCoveredQuarters();
			for (CDateRange quarterInMask : maskQuarters) {
				dc = new DateContext(
					quarterInMask,
					FeatureGroup.OUTCOME,
					index++,
					null
				);
				dcList.add(dc);
			}
		}

		return dcList;
	}

	/**
	 * Returns the date ranges that are in the specified range around the event.
	 * 
	 * @param event       The date (as days from {@link EPOCH_DAY} from which the
	 *                    relative range is calculated.
	 * @param eventIndex  Indicates to which {@link FeatureGroup} the range of the
	 *                    event belongs.
	 * @param featureTime The number of feature timeunit ranges.
	 * @param outcomeTime The number of outcome timeunit ranges.
	 * @param resultMode
	 * @param resolution
	 * @return
	 */
	public static List<DateContext> generateRelativeContexts(int event, EventIndex eventIndex, int featureTime,
			int outcomeTime, boolean sliced, Resolution resolution) {
		if (featureTime < 1 || outcomeTime < 1) {
			throw new IllegalArgumentException("Relative times were smaller than 1 (featureTime: " + featureTime
					+ "; outcomeTime: " + outcomeTime + ")");
		}
		List<DateContext> dcl = new ArrayList<>();

		CDateRange featureRange = generateFeatureRange(event, eventIndex, featureTime, resolution);
		CDateRange outcomeRange = generateOutcomeRange(event, eventIndex, outcomeTime, resolution);

		dcl.add(new DateContext(featureRange, FeatureGroup.FEATURE, null, CDate.toLocalDate(event)));
		dcl.add(new DateContext(outcomeRange, FeatureGroup.OUTCOME, null, CDate.toLocalDate(event)));

		if (sliced) {
			List<CDateRange> featureRanges = null;
			List<CDateRange> outcomeRanges = null;

			if (resolution.equals(Resolution.DAYS)) {
				featureRanges = featureRange.getCoveredDays();
				outcomeRanges = outcomeRange.getCoveredDays();
			} else if (resolution.equals(Resolution.QUARTERS)) {
				featureRanges = featureRange.getCoveredQuarters();
				outcomeRanges = outcomeRange.getCoveredQuarters();
			} else {
				throw new IllegalArgumentException("Resolution " + resolution + " not supported.");
			}

			int numRanges = featureRanges.size();
			int idx = eventIndex.equals(EventIndex.BEFORE) ? numRanges - 1 : numRanges;
			for (CDateRange range : featureRanges) {
				dcl.add(new DateContext(range, FeatureGroup.FEATURE, -idx, CDate.toLocalDate(event)));
				idx--;
			}

			numRanges = outcomeRanges.size();
			idx = eventIndex.equals(EventIndex.AFTER) ? 0 : 1;
			for (CDateRange range : outcomeRanges) {
				dcl.add(new DateContext(range, FeatureGroup.OUTCOME, idx, CDate.toLocalDate(event)));
				idx++;
			}
		}

		return dcl;
	}

	/**
	 * Calculates the feature range.
	 * 
	 * @param event       The event date to which the range is relative.
	 * @param eventIndex  Indicates to which {@link FeatureGroup} the event index
	 *                    belongs.
	 * @param featureTime the time units to be included.
	 * @param resolution  The time unit.
	 * @return The feature range.
	 */
	private static CDateRange generateFeatureRange(int event, EventIndex eventIndex, int featureTime,
			Resolution resolution) {
		if (eventIndex.equals(EventIndex.BEFORE)) {
			switch (resolution) {
				case DAYS:
					return CDateRange.of(event - featureTime + 1, event);
				case QUARTERS:
					LocalDate eventRangeStart = QuarterUtils
						.getFirstDayOfQuarter(LocalDate.ofEpochDay(event).minus(featureTime - 1, IsoFields.QUARTER_YEARS));
					LocalDate eventRangeEnd = QuarterUtils.getLastDayOfQuarter(event);
					return CDateRange.of(eventRangeStart, eventRangeEnd);
				default:
					throw new IllegalArgumentException("Unsupported Resolution: " + resolution);
			}
		}
		// eventIndex == NEUTRAL or AFTER
		else {
			switch (resolution) {
				case DAYS:
					return CDateRange.of(event - featureTime, event - 1);
				case QUARTERS:
					LocalDate eventRangeStart = QuarterUtils
							.getFirstDayOfQuarter(LocalDate.ofEpochDay(event).minus(featureTime, IsoFields.QUARTER_YEARS));
					LocalDate eventRangeEnd = QuarterUtils
							.getLastDayOfQuarter(LocalDate.ofEpochDay(event).minus(1, IsoFields.QUARTER_YEARS));
					return CDateRange.of(eventRangeStart, eventRangeEnd);
				default:
					throw new IllegalArgumentException("Unsupported Resolution: " + resolution);
			}
		}
	}

	/**
	 * Calculates the outcome range.
	 * 
	 * @param event       The event date to which the range is relative.
	 * @param eventIndex  Indicates to which {@link FeatureGroup} the event index
	 *                    belongs.
	 * @param outcomeTime the time units to be included.
	 * @param resolution  The time unit.
	 * @return The outcome range.
	 */
	private static CDateRange generateOutcomeRange(int event, EventIndex eventIndex, int outcomeTime,
			Resolution resolution) {
		if (eventIndex.equals(EventIndex.AFTER)) {
			switch (resolution) {
				case DAYS:
					return CDateRange.of(event, event + outcomeTime - 1);
				case QUARTERS:
					LocalDate eventRangeStart = QuarterUtils.getFirstDayOfQuarter(event);
					LocalDate eventRangeEnd = QuarterUtils.getLastDayOfQuarter(
							LocalDate.ofEpochDay(event).plus(outcomeTime - 1, IsoFields.QUARTER_YEARS));
					return CDateRange.of(eventRangeStart, eventRangeEnd);
				default:
					throw new IllegalArgumentException("Unsupported Resolution: " + resolution);
			}
		}
		// eventIndex == NEUTRAL or BEFORE
		else {
			switch (resolution) {
				case DAYS:
					return CDateRange.of(event + 1, event + outcomeTime);
				case QUARTERS:
					LocalDate eventRangeStart = QuarterUtils
							.getFirstDayOfQuarter(LocalDate.ofEpochDay(event).plus(1, IsoFields.QUARTER_YEARS));
					LocalDate eventRangeEnd = QuarterUtils
							.getLastDayOfQuarter(LocalDate.ofEpochDay(event).plus(outcomeTime, IsoFields.QUARTER_YEARS));
					return CDateRange.of(eventRangeStart, eventRangeEnd);
				default:
					throw new IllegalArgumentException("Unsupported Resolution: " + resolution);
			}
		}
	}
}