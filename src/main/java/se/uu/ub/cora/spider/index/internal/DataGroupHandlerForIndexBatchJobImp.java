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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;

public class DataGroupHandlerForIndexBatchJobImp implements DataGroupHandlerForIndexBatchJob {
	private static final String NUM_OF_PROCESSED_RECORDS = "numberOfProcessedRecords";
	private static final String ERROR = "error";
	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern(DATE_TIME_PATTERN);

	@Override
	public void updateDataGroup(IndexBatchJob indexBatchJob, DataGroup existingIndexBatchJob) {
		updateNumOfProcessedRecordsInDataGroup(indexBatchJob, existingIndexBatchJob);
		addIndexErrorsToDataGroup(indexBatchJob, existingIndexBatchJob);
		updateStatusWhenJobbIsFinished(indexBatchJob, existingIndexBatchJob);
		addUpdateRecordInfo(existingIndexBatchJob);
	}

	private void updateNumOfProcessedRecordsInDataGroup(IndexBatchJob indexBatchJob,
			DataGroup existingIndexBatchJob) {
		replaceAtomicChild(existingIndexBatchJob, NUM_OF_PROCESSED_RECORDS,
				String.valueOf(indexBatchJob.numberOfProcessedRecords));
	}

	private void replaceAtomicChild(DataGroup dataGroup, String nameInData, String value) {
		dataGroup.removeFirstChildWithNameInData(nameInData);
		addAtomicValueToDataGroup(nameInData, value, dataGroup);
	}

	private void addAtomicValueToDataGroup(String nameInData, String value, DataGroup dataGroup) {
		DataAtomic atomic = DataProvider.createAtomicUsingNameInDataAndValue(nameInData, value);
		dataGroup.addChild(atomic);
	}

	private void addIndexErrorsToDataGroup(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		int repeatId = initilizeRepeatIdForError(dataGroup);
		for (IndexError indexError : indexBatchJob.errors) {
			convertIndexErrorAndAddToDataGroup(dataGroup, indexError, repeatId);
			repeatId++;
		}
	}

	private int initilizeRepeatIdForError(DataGroup dataGroup) {
		return (dataGroup.getAllGroupsWithNameInData(ERROR).size()) + 1;
	}

	private void convertIndexErrorAndAddToDataGroup(DataGroup dataGroup, IndexError indexError,
			int repeatId) {
		DataGroup error = DataProvider.createGroupUsingNameInData(ERROR);
		dataGroup.addChild(error);
		error.setRepeatId(String.valueOf(repeatId));
		addAtomicValueToDataGroup("recordId", indexError.recordId, error);
		addAtomicValueToDataGroup("message", indexError.message, error);
	}

	private void updateStatusWhenJobbIsFinished(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		replaceAtomicChild(dataGroup, "status", String.valueOf(indexBatchJob.status));
	}

	private void addUpdateRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		addUpdatedDataGroup(recordInfo);
	}

	private void addUpdatedDataGroup(DataGroup recordInfo) {
		DataGroup updatedDataGroup = DataProvider.createGroupUsingNameInData("updated");
		createAndAddUpdatedBy(updatedDataGroup);
		createAndAddTsUpdated(updatedDataGroup);
		setRepeatIdOnUpdated(recordInfo, updatedDataGroup);
		recordInfo.addChild(updatedDataGroup);
	}

	private void setRepeatIdOnUpdated(DataGroup recordInfo, DataGroup updatedDataGroup) {
		int numberOfUpdated = recordInfo.getAllGroupsWithNameInData("updated").size();
		updatedDataGroup.setRepeatId(String.valueOf(numberOfUpdated + 1));
	}

	private void createAndAddUpdatedBy(DataGroup updatedDataGroup) {
		DataRecordLink updatedBy = DataProvider
				.createRecordLinkUsingNameInDataAndTypeAndId("updatedBy", "user", "system");
		updatedDataGroup.addChild(updatedBy);
	}

	private void createAndAddTsUpdated(DataGroup updatedDataGroup) {
		DataAtomic tsUpdated = DataProvider.createAtomicUsingNameInDataAndValue("tsUpdated",
				getCurrentFormattedTime());
		updatedDataGroup.addChild(tsUpdated);
	}

	private static String getCurrentFormattedTime() {
		LocalDateTime currentDateTime = LocalDateTime.now();
		return currentDateTime.format(dateTimeFormatter);
	}

	@Override
	public DataGroup createDataGroup(IndexBatchJob indexBatchJob) {
		DataGroup dataGroup = DataProvider.createGroupUsingNameInData("indexBatchJob");
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
		DataGroup recordInfo = DataProvider.createGroupUsingNameInData("recordInfo");
		DataRecordLink dataDivider = DataProvider
				.createRecordLinkUsingNameInDataAndTypeAndId("dataDivider", "system", "cora");
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
