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

	private DataAtomicFactorySpy atomicFactory;
	private IndexBatchJob indexBatchJob;
	private DataGroupSpy indexBatchJobDataGroup;
	private IndexBatchJobConverter converter;
	private DataGroupFactorySpy dataGroupFactory;

	@BeforeMethod
	public void setUp() {
		atomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(atomicFactory);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		indexBatchJob = createIndexBatchJob();
		indexBatchJobDataGroup = createDataGroup();
		converter = new IndexBatchJobConverter();
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
		List<DataGroup> errors2 = indexBatchJobDataGroup.getAllGroupsWithNameInData("error");
		assertEquals(errors2.size(), 3);

		assertCorrectError(0, "0", "someRecordId", "some read error message");
		assertCorrectError(1, "1", "recordIdOne", "some index error message");
		assertCorrectError(2, "2", "recordIdTwo", "some other index error message");
	}

	@Test
	public void testStatusStartedDoesNotOverwriteStorage() {
		indexBatchJob.status = "started";

		assertStatusInUpdatedDataGroupEquals("someStatus");
	}

	@Test
	public void testStatusPausedDoesNotOverwriteStorage() {
		indexBatchJob.status = "paused";

		assertStatusInUpdatedDataGroupEquals("someStatus");
	}

	@Test
	public void testStatusFinishedDoesOverwriteStorage() {
		indexBatchJob.status = "finished";

		assertStatusInUpdatedDataGroupEquals("finished");
	}

	private void assertStatusInUpdatedDataGroupEquals(String expected) {
		converter.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
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
		IndexBatchJob indexBatchJob = new IndexBatchJob("someRecordType", "someRecordId", filter);
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

}
