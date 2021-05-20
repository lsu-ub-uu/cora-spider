/*
 * Copyright 2021 Uppsala University Library
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
package se.uu.ub.cora.spider.index.internal;

import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;

public class IndexBatchJobConverter implements BatchJobConverter {

	private static final String NUM_OF_PROCESSED_RECORDS = "numOfProcessedRecords";
	private static final String ERROR = "error";

	@Override
	public void updateDataGroup(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		updateNumOfProcessedRecordsInDataGroup(indexBatchJob, dataGroup);
		addIndexErrorsToDataGroup(indexBatchJob, dataGroup);
		possiblyUpdateStatus(indexBatchJob, dataGroup);
	}

	private void updateNumOfProcessedRecordsInDataGroup(IndexBatchJob indexBatchJob,
			DataGroup dataGroup) {
		replaceAtomicChild(dataGroup, NUM_OF_PROCESSED_RECORDS,
				String.valueOf(indexBatchJob.numOfProcessedRecords));
	}

	private void replaceAtomicChild(DataGroup dataGroup, String nameInData, String value) {
		dataGroup.removeFirstChildWithNameInData(nameInData);
		createAndAddAtomicValue(nameInData, value, dataGroup);
	}

	private void createAndAddAtomicValue(String nameInData, String value, DataGroup dataGroup) {
		DataAtomic atomicChild = DataAtomicProvider.getDataAtomicUsingNameInDataAndValue(nameInData,
				value);
		dataGroup.addChild(atomicChild);
	}

	private void addIndexErrorsToDataGroup(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		createAndAddErrors(indexBatchJob, dataGroup);
		setConsecutiveRepeatIdsForErrors(dataGroup);
	}

	private void createAndAddErrors(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		for (IndexError indexError : indexBatchJob.errors) {
			convertIndexErrorAndAddToDataGroup(dataGroup, indexError);
		}
	}

	private void convertIndexErrorAndAddToDataGroup(DataGroup dataGroup, IndexError indexError) {
		DataGroup errorDataGroup = DataGroupProvider.getDataGroupUsingNameInData(ERROR);
		createAndAddAtomicValue("recordId", indexError.recordId, errorDataGroup);
		createAndAddAtomicValue("message", indexError.message, errorDataGroup);
		dataGroup.addChild(errorDataGroup);
	}

	private void setConsecutiveRepeatIdsForErrors(DataGroup dataGroup) {
		List<DataGroup> errors = dataGroup.getAllGroupsWithNameInData(ERROR);
		for (int i = 0; i < errors.size(); i++) {
			DataGroup errorDataGroup = errors.get(i);
			errorDataGroup.setRepeatId(String.valueOf(i));
		}
	}

	private void possiblyUpdateStatus(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		if (newStatusIsFinished(indexBatchJob)) {
			replaceAtomicChild(dataGroup, "status", String.valueOf(indexBatchJob.status));
		}
	}

	private boolean newStatusIsFinished(IndexBatchJob indexBatchJob) {
		return "finished".equals(indexBatchJob.status);
	}

	@Override
	public DataGroup createDataGroup(IndexBatchJob indexBatchJob) {
		DataGroup dataGroup = DataGroupProvider.getDataGroupUsingNameInData("indexBatchJob");
		createAndAddRecordInfo(dataGroup);
		createAndAddRecordTypeToIndex(indexBatchJob, dataGroup);
		createAndAddRecordStatus(indexBatchJob, dataGroup);
		createAndAddNumberOfProcessedRecords(indexBatchJob, dataGroup);
		createAndAddTotalNumberToIndex(indexBatchJob, dataGroup);
		addFilter(indexBatchJob, dataGroup);
		addIndexErrorsToDataGroup(indexBatchJob, dataGroup);
		return dataGroup;
	}

	private void createAndAddRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = DataGroupProvider.getDataGroupUsingNameInData("recordInfo");
		DataGroup dataDivider = DataGroupProvider
				.getDataGroupAsLinkUsingNameInDataTypeAndId("dataDivider", "system", "cora");
		recordInfo.addChild(dataDivider);
		dataGroup.addChild(recordInfo);
	}

	private void createAndAddRecordTypeToIndex(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		createAndAddAtomicValue("recordType", indexBatchJob.recordTypeToIndex, dataGroup);
	}

	private void createAndAddRecordStatus(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		createAndAddAtomicValue("status", indexBatchJob.status, dataGroup);
	}

	private void createAndAddNumberOfProcessedRecords(IndexBatchJob indexBatchJob,
			DataGroup dataGroup) {
		long numOfProcessedRecords = indexBatchJob.numOfProcessedRecords;
		createAndAddAtomicValue(NUM_OF_PROCESSED_RECORDS, String.valueOf(numOfProcessedRecords),
				dataGroup);
	}

	private void createAndAddTotalNumberToIndex(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		long totalNumberToIndex = indexBatchJob.totalNumberToIndex;
		createAndAddAtomicValue("totalNumberToIndex", String.valueOf(totalNumberToIndex),
				dataGroup);
	}

	private void addFilter(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		dataGroup.addChild(indexBatchJob.filter);
	}

	// TODO: is this needed?
	// public IndexBatchJob createIndexBatchJob(DataGroup dataGroup) {
	// IndexBatchJob indexBatchJob = new IndexBatchJob("", "",
	// DataGroupProvider.getDataGroupUsingNameInData("filter"));
	// if (dataGroup.containsChildWithNameInData(NUM_OF_PROCESSED_RECORDS)) {
	// String numOfRecords = dataGroup
	// .getFirstAtomicValueWithNameInData(NUM_OF_PROCESSED_RECORDS);
	// indexBatchJob.numOfProcessedRecords = Long.valueOf(numOfRecords);
	// }
	// return indexBatchJob;
	// }

}
