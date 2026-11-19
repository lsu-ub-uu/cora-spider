/*
 * Copyright 2026 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.spider.extended.batchjob;

import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordStorage;

public class CalculateNumberToIndexExtendedFunctionality implements ExtendedFunctionality {
	private static final String FILTER = "filter";
	private SpiderDependencyProvider dependencyProvider;

	public CalculateNumberToIndexExtendedFunctionality(
			SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		Filter filter = getFilter(dataRecordGroup);
		long totalNumberOfMatches = getTotalNumberOfMatches(dataRecordGroup, filter);

		data.dataSharer.put("IndexBatchJob_filter", filter);
		data.dataSharer.put("IndexBatchJob_totalNumberOfMatches", totalNumberOfMatches);

		dataRecordGroup.addChild(createAtomic(totalNumberOfMatches));
	}

	private Filter getFilter(DataRecordGroup dataRecordGroup) {
		DataGroup filterDG = extractFilterFromIndexSettingsOrCreateANewOne(dataRecordGroup);
		DataGroupToFilter converterDataGroupToFilter = dependencyProvider
				.getDataGroupToFilterConverter();
		return converterDataGroupToFilter.convert(filterDG);
	}

	private DataGroup extractFilterFromIndexSettingsOrCreateANewOne(
			DataRecordGroup dataRecordGroup) {
		if (dataRecordGroup.containsChildWithNameInData(FILTER)) {
			return extractFilterFromIndexSettings(dataRecordGroup);
		}
		return createEmptyFilter();
	}

	private DataGroup createEmptyFilter() {
		return DataProvider.createGroupUsingNameInData(FILTER);
	}

	private DataGroup extractFilterFromIndexSettings(DataRecordGroup indexSettings) {
		return indexSettings.getFirstGroupWithNameInData(FILTER);
	}

	private long getTotalNumberOfMatches(DataRecordGroup dataRecordGroup, Filter filter) {
		String recordTypeForBatch = getRecordTypeToIndex(dataRecordGroup);
		return getTotalNumberOfMatchesFromStorageUsingFilter(recordTypeForBatch, filter);
	}

	private String getRecordTypeToIndex(DataRecordGroup dataRecordGroup) {
		return dataRecordGroup.getFirstAtomicValueWithNameInData("recordTypeToIndex");
	}

	private long getTotalNumberOfMatchesFromStorageUsingFilter(String recordTypeToIndex,
			Filter filter) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		return recordStorage.getTotalNumberOfRecordsForTypes(List.of(recordTypeToIndex), filter);
	}

	private DataAtomic createAtomic(long totalNumberOfMatches) {
		return DataProvider.createAtomicUsingNameInDataAndValue("totalNumberToIndex",
				String.valueOf(totalNumberOfMatches));
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

}
