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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class IndexBatchJobConverterTest {

	private static final String SOME_RECORD_TYPE = "someRecordType";
	private DataAtomicFactorySpy atomicFactory;
	private IndexBatchJob indexBatchJob;
	private DataGroupSpy indexBatchJobDataGroup;
	private IndexBatchJobConverter converter;
	private DataGroupFactorySpy dataGroupFactory;

	@BeforeMethod
	public void setUp() {
		setUpProviders();

		indexBatchJob = createIndexBatchJob();
		indexBatchJobDataGroup = createDataGroup();
		converter = new IndexBatchJobConverter();
	}

	private void setUpProviders() {
		atomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(atomicFactory);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
	}

	@Test
	public void testUpdateNumOfProcessedRecordsInDataGroup() {
		converter.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
		assertEquals(indexBatchJobDataGroup.removedNameInDatas.get(0), "numOfProcessedRecords");
		assertEquals(atomicFactory.nameInDatas.get(0), "numOfProcessedRecords");
		assertEquals(
				indexBatchJobDataGroup.getFirstAtomicValueWithNameInData("numOfProcessedRecords"),
				"67");
	}

	@Test
	public void testUpdateErrorsinDataGroup() {
		converter.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
		List<DataGroup> errors = indexBatchJobDataGroup.getAllGroupsWithNameInData("error");
		assertEquals(errors.size(), 3);

		assertCorrectError(0, "0", "someRecordId", "some read error message");
		assertCorrectError(1, "1", "recordIdOne", "some index error message");
		assertCorrectError(2, "2", "recordIdTwo", "some other index error message");
	}

	@Test
	public void testStatusStartedDoesNotOverwriteStorage() {
		indexBatchJob.status = "started";
		converter.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);

		assertStatusInUpdatedDataGroupEquals("someStatus");
	}

	@Test
	public void testStatusPausedDoesNotOverwriteStorage() {
		indexBatchJob.status = "paused";
		converter.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);

		assertStatusInUpdatedDataGroupEquals("someStatus");
	}

	@Test
	public void testStatusFinishedDoesOverwriteStorage() {
		indexBatchJob.status = "finished";
		converter.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);

		assertStatusInUpdatedDataGroupEquals("finished");
	}

	private void assertStatusInUpdatedDataGroupEquals(String expected) {
		assertEquals(indexBatchJobDataGroup.getFirstAtomicValueWithNameInData("status"), expected);
	}

	private void assertCorrectError(int index, String repeatId, String recordId, String message) {
		List<DataGroup> errors = indexBatchJobDataGroup.getAllGroupsWithNameInData("error");
		DataGroup error = errors.get(index);
		assertEquals(error.getRepeatId(), repeatId);
		assertEquals(error.getFirstAtomicValueWithNameInData("recordId"), recordId);
		assertEquals(error.getFirstAtomicValueWithNameInData("message"), message);
	}

	private DataGroupSpy createDataGroup() {
		DataGroupSpy indexBatchJobDataGroup = new DataGroupSpy("indexBatchJob");
		indexBatchJobDataGroup.addChild(new DataAtomicSpy("numOfProcessedRecords", "34"));
		indexBatchJobDataGroup.addChild(new DataAtomicSpy("totalNumberToIndex", "34"));
		indexBatchJobDataGroup.addChild(new DataAtomicSpy("status", "someStatus"));
		createAndAddErrorDataGroup(indexBatchJobDataGroup);
		return indexBatchJobDataGroup;
	}

	private void createAndAddErrorDataGroup(DataGroupSpy indexBatchJobDataGroup) {
		DataGroupSpy error = new DataGroupSpy("error");
		error.setRepeatId("0");
		error.addChild(new DataAtomicSpy("recordId", "someRecordId"));
		error.addChild(new DataAtomicSpy("message", "some read error message"));
		indexBatchJobDataGroup.addChild(error);
	}

	private IndexBatchJob createIndexBatchJob() {
		DataGroupSpy filter = new DataGroupSpy("filter");
		IndexBatchJob indexBatchJob = new IndexBatchJob(SOME_RECORD_TYPE, 10, filter);
		indexBatchJob.numOfProcessedRecords = 67;
		indexBatchJob.status = "started";
		indexBatchJob.totalNumberToIndex = 198;
		createAndAddErrors(indexBatchJob);
		return indexBatchJob;
	}

	private void createAndAddErrors(IndexBatchJob indexBatchJob) {
		List<IndexError> errors = new ArrayList<>();
		errors.add(new IndexError("recordIdOne", "some index error message"));
		errors.add(new IndexError("recordIdTwo", "some other index error message"));
		indexBatchJob.errors = errors;
	}

	@Test
	public void testCreateDataGroupFromIndexBatchJob() {
		DataGroup createdDataGroup = converter.createDataGroup(indexBatchJob);
		assertEquals(createdDataGroup.getNameInData(), "indexBatchJob");

		assertCorrectRecordInfo(createdDataGroup);
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("recordType"),
				SOME_RECORD_TYPE);
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("status"), "started");
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("numOfProcessedRecords"),
				"67");
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("totalNumberToIndex"),
				"198");
		assertSame(createdDataGroup.getFirstGroupWithNameInData("filter"), indexBatchJob.filter);

		List<DataGroup> dataGroupErrors = createdDataGroup.getAllGroupsWithNameInData("error");

		assertCorrectSetErrorsInDataGroup(dataGroupErrors);
	}

	private void assertCorrectRecordInfo(DataGroup createdDataGroup) {
		DataGroup recordInfo = createdDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup dataDivider = recordInfo.getFirstGroupWithNameInData("dataDivider");
		assertEquals(dataDivider.getFirstAtomicValueWithNameInData("linkedRecordType"), "system");
		assertEquals(dataDivider.getFirstAtomicValueWithNameInData("linkedRecordId"), "cora");
		assertFalse(recordInfo.containsChildWithNameInData("id"));
	}

	private void assertCorrectSetErrorsInDataGroup(List<DataGroup> dataGroupErrors) {
		List<IndexError> indexErrors = indexBatchJob.errors;
		assertCorrectErrorInDataGroup(dataGroupErrors, indexErrors, 0);
		assertCorrectErrorInDataGroup(dataGroupErrors, indexErrors, 1);

		assertEquals(dataGroupErrors.size(), 2);
	}

	private void assertCorrectErrorInDataGroup(List<DataGroup> dataGroupErrors,
			List<IndexError> indexErrors, int index) {
		DataGroup error = dataGroupErrors.get(index);
		assertEquals(error.getFirstAtomicValueWithNameInData("recordId"),
				indexErrors.get(index).recordId);
		assertEquals(error.getFirstAtomicValueWithNameInData("message"),
				indexErrors.get(index).message);
		assertEquals(error.getRepeatId(), String.valueOf(index));
	}

	@Test
	public void testCreateDataGroupFromIndexBatchJobEmptyErrors() {
		DataGroup createdDataGroup = converter
				.createDataGroup(new IndexBatchJob("place", 10, new DataGroupSpy("filter")));
		assertEquals(createdDataGroup.getNameInData(), "indexBatchJob");
		assertFalse(createdDataGroup.containsChildWithNameInData("error"));

	}

	// TODO: is this needed?
	// @Test
	// public void testCreateIndexBatchJobFromDataGroup() {
	// IndexBatchJob indexBatchJob = converter.createIndexBatchJob(indexBatchJobDataGroup);
	// // assertEquals(indexBatchJob.filter.getNameInData(), "filter");
	// // assertEquals(indexBatchJob.recordType, "");
	// // assertEquals(indexBatchJob.recordId, "");
	// // assertEquals(indexBatchJob.errors, Collections.emptyList());
	// // assertEquals(indexBatchJob.status, "started");
	// assertEquals(indexBatchJob.numOfProcessedRecords, 34);
	// // assertEquals(indexBatchJob.totalNumberToIndex, 0);
	// }
	//
	// @Test
	// public void testCreateIndexBatchJobFromEmptyDataGroup() {
	// DataGroupSpy emptyDataGroup = new DataGroupSpy("indexBatchJob");
	// IndexBatchJob indexBatchJob = converter.createIndexBatchJob(emptyDataGroup);
	// assertEquals(indexBatchJob.filter.getNameInData(), "filter");
	// assertEquals(indexBatchJob.recordType, "");
	// assertEquals(indexBatchJob.recordId, "");
	// assertEquals(indexBatchJob.errors, Collections.emptyList());
	// assertEquals(indexBatchJob.status, "started");
	// assertEquals(indexBatchJob.numOfProcessedRecords, 34);
	// assertEquals(indexBatchJob.totalNumberToIndex, 0);
	// }

}
