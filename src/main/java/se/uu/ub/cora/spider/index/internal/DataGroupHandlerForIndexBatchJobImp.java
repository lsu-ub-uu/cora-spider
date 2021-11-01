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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;

public class DataGroupHandlerForIndexBatchJobImp implements DataGroupHandlerForIndexBatchJob {
	private static final String NUM_OF_PROCESSED_RECORDS = "numberOfProcessedRecords";
	private static final String ERROR = "error";

	@Override
	public void updateDataGroup(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		updateNumOfProcessedRecordsInDataGroup(indexBatchJob, dataGroup);
		addIndexErrorsToDataGroup(indexBatchJob, dataGroup);
		possiblyUpdateStatus(indexBatchJob, dataGroup);
		addUpdateInfo(dataGroup);
	}

	private void updateNumOfProcessedRecordsInDataGroup(IndexBatchJob indexBatchJob,
			DataGroup dataGroup) {
		replaceAtomicChild(dataGroup, NUM_OF_PROCESSED_RECORDS,
				String.valueOf(indexBatchJob.numberOfProcessedRecords));
	}

	private void replaceAtomicChild(DataGroup dataGroup, String nameInData, String value) {
		dataGroup.removeFirstChildWithNameInData(nameInData);
		addAtomicValueToDataGroup(nameInData, value, dataGroup);
	}

	private void addAtomicValueToDataGroup(String nameInData, String value, DataGroup dataGroup) {
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
		addAtomicValueToDataGroup("recordId", indexError.recordId, errorDataGroup);
		addAtomicValueToDataGroup("message", indexError.message, errorDataGroup);
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

	private void addUpdateInfo(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");

		addUpdatedDataGroup(recordInfo);
		setNewRepeatIdsForUpdatedGroups(recordInfo);
	}

	private void addUpdatedDataGroup(DataGroup recordInfo) {
		DataGroup updatedDataGroup = DataGroupProvider.getDataGroupUsingNameInData("updated");
		createAndAddUpdatedBy(recordInfo, updatedDataGroup);
		createAndAddTsUpdated(updatedDataGroup);
		recordInfo.addChild(updatedDataGroup);
	}

	private void createAndAddUpdatedBy(DataGroup recordInfo, DataGroup updatedDataGroup) {
		DataGroup updatedBy = createUpdatedByUsingRecordInfo(recordInfo);
		updatedDataGroup.addChild(updatedBy);
	}

	private DataGroup createUpdatedByUsingRecordInfo(DataGroup recordInfo) {
		DataGroup createdBy = recordInfo.getFirstGroupWithNameInData("createdBy");
		String userType = createdBy.getFirstAtomicValueWithNameInData("linkedRecordType");
		String userId = createdBy.getFirstAtomicValueWithNameInData("linkedRecordId");
		return DataGroupProvider.getDataGroupAsLinkUsingNameInDataTypeAndId("updatedBy", userType,
				userId);
	}

	private void createAndAddTsUpdated(DataGroup updatedDataGroup) {
		String currentLocalDateTime = getCurrentTimestampAsString();
		updatedDataGroup.addChild(DataAtomicProvider
				.getDataAtomicUsingNameInDataAndValue("tsUpdated", currentLocalDateTime));
	}

	protected String getCurrentTimestampAsString() {
		return formatInstantKeepingTrailingZeros(Instant.now());
	}

	protected String formatInstantKeepingTrailingZeros(Instant instant) {
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
		return formatter.format(instant);
	}

	private void setNewRepeatIdsForUpdatedGroups(DataGroup recordInfo) {
		int repeatIdCounter = 0;
		for (DataGroup updated : recordInfo.getAllGroupsWithNameInData("updated")) {
			updated.setRepeatId(String.valueOf(repeatIdCounter));
			repeatIdCounter++;
		}
	}

	@Override
	public DataGroup createDataGroup(IndexBatchJob indexBatchJob) {
		DataGroup dataGroup = DataGroupProvider.getDataGroupUsingNameInData("indexBatchJob");
		addRecordInfo(dataGroup);
		addRecordTypeToIndex(indexBatchJob, dataGroup);
		addRecordStatus(indexBatchJob, dataGroup);
		addNumberOfProcessedRecords(indexBatchJob, dataGroup);
		addTotalNumberToIndex(indexBatchJob, dataGroup);
		possiblyAddFilter(indexBatchJob, dataGroup);
		addIndexErrorsToDataGroup(indexBatchJob, dataGroup);
		return dataGroup;
	}

	private void addRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = DataGroupProvider.getDataGroupUsingNameInData("recordInfo");
		DataGroup dataDivider = DataGroupProvider
				.getDataGroupAsLinkUsingNameInDataTypeAndId("dataDivider", "system", "cora");
		recordInfo.addChild(dataDivider);
		dataGroup.addChild(recordInfo);
	}

	private void addRecordTypeToIndex(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		addAtomicValueToDataGroup("recordTypeToIndex", indexBatchJob.recordTypeToIndex, dataGroup);
	}

	private void addRecordStatus(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		addAtomicValueToDataGroup("status", indexBatchJob.status, dataGroup);
	}

	private void addNumberOfProcessedRecords(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		long numOfProcessedRecords = indexBatchJob.numberOfProcessedRecords;
		addAtomicValueToDataGroup(NUM_OF_PROCESSED_RECORDS, String.valueOf(numOfProcessedRecords),
				dataGroup);
	}

	private void addTotalNumberToIndex(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		long totalNumberToIndex = indexBatchJob.totalNumberToIndex;
		addAtomicValueToDataGroup("totalNumberToIndex", String.valueOf(totalNumberToIndex),
				dataGroup);
	}

	private void possiblyAddFilter(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		if (indexBatchJob.filter.hasChildren()) {
			dataGroup.addChild(indexBatchJob.filter);
		}
	}
}
