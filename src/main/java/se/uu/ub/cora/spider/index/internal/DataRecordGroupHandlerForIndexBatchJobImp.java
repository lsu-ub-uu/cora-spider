/*
 * Copyright 2021, 2023, 2025 Uppsala University Library
 * Copyright 2024 Olov McKie
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
import se.uu.ub.cora.data.DataParent;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;

public class DataRecordGroupHandlerForIndexBatchJobImp
		implements DataRecordGroupHandlerForIndexBatchJob {
	private static final String NUM_OF_PROCESSED_RECORDS = "numberOfProcessedRecords";
	private static final String ERROR = "error";
	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern(DATE_TIME_PATTERN);

	@Override
	public void updateDataRecordGroup(IndexBatchJob indexBatchJob,
			DataRecordGroup existingIndexBatchJob) {
		updateNumOfProcessedRecordsInDataGroup(indexBatchJob, existingIndexBatchJob);
		addIndexErrorsToDataGroup(indexBatchJob, existingIndexBatchJob);
		updateStatusWhenJobbIsFinished(indexBatchJob, existingIndexBatchJob);
		addUpdateRecordInfo(existingIndexBatchJob);
	}

	private void updateNumOfProcessedRecordsInDataGroup(IndexBatchJob indexBatchJob,
			DataRecordGroup existingIndexBatchJob) {
		replaceAtomicChild(existingIndexBatchJob, NUM_OF_PROCESSED_RECORDS,
				String.valueOf(indexBatchJob.numberOfProcessedRecords));
	}

	private void replaceAtomicChild(DataRecordGroup existingIndexBatchJob, String nameInData,
			String value) {
		existingIndexBatchJob.removeFirstChildWithNameInData(nameInData);
		addAtomicValueToDataGroup(nameInData, value, existingIndexBatchJob);
	}

	private void addAtomicValueToDataGroup(String nameInData, String value,
			DataParent existingIndexBatchJob) {
		DataAtomic atomic = DataProvider.createAtomicUsingNameInDataAndValue(nameInData, value);
		existingIndexBatchJob.addChild(atomic);
	}

	private void addIndexErrorsToDataGroup(IndexBatchJob indexBatchJob,
			DataParent existingIndexBatchJob) {
		int repeatId = initilizeRepeatIdForError(existingIndexBatchJob);
		for (IndexError indexError : indexBatchJob.errors) {
			convertIndexErrorAndAddToDataGroup(existingIndexBatchJob, indexError, repeatId);
			repeatId++;
		}
	}

	private int initilizeRepeatIdForError(DataParent existingIndexBatchJob) {
		return (existingIndexBatchJob.getAllGroupsWithNameInData(ERROR).size()) + 1;
	}

	private void convertIndexErrorAndAddToDataGroup(DataParent existingIndexBatchJob,
			IndexError indexError, int repeatId) {
		DataGroup error = DataProvider.createGroupUsingNameInData(ERROR);
		existingIndexBatchJob.addChild(error);
		error.setRepeatId(String.valueOf(repeatId));
		addAtomicValueToDataGroup("recordId", indexError.recordId, error);
		addAtomicValueToDataGroup("message", indexError.message, error);
	}

	private void updateStatusWhenJobbIsFinished(IndexBatchJob indexBatchJob,
			DataRecordGroup existingIndexBatchJob) {
		replaceAtomicChild(existingIndexBatchJob, "status", String.valueOf(indexBatchJob.status));
	}

	private void addUpdateRecordInfo(DataRecordGroup existingIndexBatchJob) {
		DataGroup recordInfo = existingIndexBatchJob.getFirstGroupWithNameInData("recordInfo");
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
	public DataRecordGroup createDataRecordGroup(IndexBatchJob indexBatchJob,
			DataGroup filterAsDataGroup) {
		DataRecordGroup dataRecordGroup = DataProvider
				.createRecordGroupUsingNameInData("indexBatchJob");
		addRecordInfo(dataRecordGroup);
		addRecordTypeToIndex(indexBatchJob, dataRecordGroup);
		addRecordStatus(indexBatchJob, dataRecordGroup);
		addNumberOfProcessedRecords(indexBatchJob, dataRecordGroup);
		addTotalNumberToIndex(indexBatchJob, dataRecordGroup);
		possiblyAddFilter(dataRecordGroup, filterAsDataGroup);
		addIndexErrorsToDataGroup(indexBatchJob, dataRecordGroup);
		return dataRecordGroup;
	}

	private void addRecordInfo(DataRecordGroup dataRecordGroup) {
		dataRecordGroup.setDataDivider("coraData");
		dataRecordGroup.setValidationType("indexBatchJob");
	}

	private void addRecordTypeToIndex(IndexBatchJob indexBatchJob,
			DataRecordGroup dataRecordGroup) {
		addAtomicValueToDataGroup("recordTypeToIndex", indexBatchJob.recordTypeToIndex,
				dataRecordGroup);
	}

	private void addRecordStatus(IndexBatchJob indexBatchJob, DataRecordGroup dataRecordGroup) {
		addAtomicValueToDataGroup("status", indexBatchJob.status, dataRecordGroup);
	}

	private void addNumberOfProcessedRecords(IndexBatchJob indexBatchJob,
			DataRecordGroup dataRecordGroup) {
		long numOfProcessedRecords = indexBatchJob.numberOfProcessedRecords;
		addAtomicValueToDataGroup(NUM_OF_PROCESSED_RECORDS, String.valueOf(numOfProcessedRecords),
				dataRecordGroup);
	}

	private void addTotalNumberToIndex(IndexBatchJob indexBatchJob,
			DataRecordGroup dataRecordGroup) {
		long totalNumberToIndex = indexBatchJob.totalNumberToIndex;
		addAtomicValueToDataGroup("totalNumberToIndex", String.valueOf(totalNumberToIndex),
				dataRecordGroup);
	}

	private void possiblyAddFilter(DataRecordGroup dataRecordGroup, DataGroup filterAsDataGroup) {
		if (filterAsDataGroup.hasChildren()) {
			dataRecordGroup.addChild(filterAsDataGroup);
		}
	}
}
