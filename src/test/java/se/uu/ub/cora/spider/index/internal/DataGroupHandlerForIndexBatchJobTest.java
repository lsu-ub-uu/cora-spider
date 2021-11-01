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
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class DataGroupHandlerForIndexBatchJobTest {

	private static final String SOME_RECORD_TYPE = "someRecordType";
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";

	private DataAtomicFactorySpy atomicFactory;
	private IndexBatchJob indexBatchJob;
	private DataGroupSpy indexBatchJobDataGroup;
	private DataGroupHandlerForIndexBatchJobImp dataGroupHandler;
	private DataGroupFactorySpy dataGroupFactory;

	@BeforeMethod
	public void setUp() {
		setUpProviders();

		indexBatchJob = createIndexBatchJob();
		indexBatchJobDataGroup = createDataGroup();
		dataGroupHandler = new DataGroupHandlerForIndexBatchJobImp();
	}

	private void setUpProviders() {
		atomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(atomicFactory);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
	}

	@Test
	public void testSetUpdateTimestampOnUpdate() {

		dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);

		assertDataGroupFactoryFactoredCorrectly();

		DataGroupSpy addedUpdatedGroup = (DataGroupSpy) dataGroupFactory.MCR
				.getReturnValue("factorUsingNameInData", 2);

		DataGroupSpy addedUpdatedBy = (DataGroupSpy) dataGroupFactory.MCR
				.getReturnValue("factorAsLinkWithNameInDataTypeAndId", 0);

		DataElement addedChildToUpdated = addedUpdatedGroup.addedChildren.get(0);
		assertSame(addedChildToUpdated, addedUpdatedBy);

		assertCorrectTsUpdated(addedUpdatedGroup);

		assertEquals(addedUpdatedGroup.repeatId, "1");
	}

	private void assertCorrectTsUpdated(DataGroupSpy addedUpdatedGroup) {
		DataAtomic tsUpdated = (DataAtomic) addedUpdatedGroup.addedChildren.get(1);
		assertTrue(tsUpdated.getValue().matches(TIMESTAMP_FORMAT));

		DataGroup recordInfo = indexBatchJobDataGroup.getFirstGroupWithNameInData("recordInfo");
		String tsCreated = recordInfo.getFirstAtomicValueWithNameInData("tsCreated");
		assertFalse(tsUpdated.getValue().equals(tsCreated));
	}

	private void assertDataGroupFactoryFactoredCorrectly() {
		dataGroupFactory.MCR.assertParameter("factorUsingNameInData", 2, "nameInData", "updated");
		dataGroupFactory.MCR.assertParameter("factorAsLinkWithNameInDataTypeAndId", 0, "nameInData",
				"updatedBy");
		dataGroupFactory.MCR.assertParameter("factorAsLinkWithNameInDataTypeAndId", 0, "recordType",
				"user");
		dataGroupFactory.MCR.assertParameter("factorAsLinkWithNameInDataTypeAndId", 0, "recordId",
				"someSuperUser");
	}

	@Test
	public void testRepeatIdInUpdatedDataGroups() {
		DataGroup recordInfo = indexBatchJobDataGroup.getFirstGroupWithNameInData("recordInfo");
		addUpdatedToRecordInfo(recordInfo, "1");
		addUpdatedToRecordInfo(recordInfo, "3");
		addUpdatedToRecordInfo(recordInfo, "7");

		dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
		DataGroupSpy addedUpdatedGroup = (DataGroupSpy) dataGroupFactory.MCR
				.getReturnValue("factorUsingNameInData", 2);
		assertEquals(addedUpdatedGroup.repeatId, "4");

		assertRepeatIdsWereReplacedWhenNewGroupWasAdded(recordInfo);

	}

	private void assertRepeatIdsWereReplacedWhenNewGroupWasAdded(DataGroup recordInfo) {
		List<DataGroup> updatedGroups = recordInfo.getAllGroupsWithNameInData("updated");
		assertEquals(updatedGroups.get(0).getRepeatId(), "0");
		assertEquals(updatedGroups.get(1).getRepeatId(), "1");
		assertEquals(updatedGroups.get(2).getRepeatId(), "2");
		assertEquals(updatedGroups.get(3).getRepeatId(), "3");
		assertEquals(updatedGroups.get(4).getRepeatId(), "4");
	}

	private void addUpdatedToRecordInfo(DataGroup recordInfo, String repeatId) {
		DataGroupSpy updated = new DataGroupSpy("updated");
		updated.setRepeatId(repeatId);
		recordInfo.addChild(updated);
	}

	@Test
	public void testUpdateNumOfProcessedRecordsInDataGroup() {
		indexBatchJob.numberOfProcessedRecords = 67;

		dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
		assertEquals(indexBatchJobDataGroup.removedNameInDatas.get(0), "numberOfProcessedRecords");
		assertEquals(atomicFactory.nameInDatas.get(0), "numberOfProcessedRecords");
		assertEquals(indexBatchJobDataGroup
				.getFirstAtomicValueWithNameInData("numberOfProcessedRecords"), "67");
	}

	@Test
	public void testUpdateErrorsinDataGroup() {
		dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
		List<DataGroup> errors = indexBatchJobDataGroup.getAllGroupsWithNameInData("error");
		assertEquals(errors.size(), 3);

		assertCorrectError(0, "0", "someRecordId", "some read error message");
		assertCorrectError(1, "1", "recordIdOne", "some index error message");
		assertCorrectError(2, "2", "recordIdTwo", "some other index error message");
	}

	@Test
	public void testStatusStartedDoesNotOverwriteStorage() {
		indexBatchJob.status = "started";
		dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);

		assertStatusInUpdatedDataGroupEquals("someStatus");
	}

	@Test
	public void testStatusPausedDoesNotOverwriteStorage() {
		indexBatchJob.status = "paused";
		dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);

		assertStatusInUpdatedDataGroupEquals("someStatus");
	}

	@Test
	public void testStatusFinishedDoesOverwriteStorage() {
		indexBatchJob.status = "finished";
		dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);

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
		DataGroupSpy recordInfo = createRecordInfo();
		indexBatchJobDataGroup.addChild(recordInfo);
		return indexBatchJobDataGroup;
	}

	private DataGroupSpy createRecordInfo() {
		DataGroupSpy recordInfo = new DataGroupSpy("recordInfo");
		DataGroupSpy createdBy = new DataGroupSpy("createdBy", "user", "someSuperUser");
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicSpy("tsCreated", "2021-05-03T14:12:11.657482Z"));
		createAndAddUpdatedDataGroup(recordInfo);
		return recordInfo;
	}

	private void createAndAddErrorDataGroup(DataGroupSpy indexBatchJobDataGroup) {
		DataGroupSpy error = new DataGroupSpy("error");
		error.setRepeatId("0");
		error.addChild(new DataAtomicSpy("recordId", "someRecordId"));
		error.addChild(new DataAtomicSpy("message", "some read error message"));
		indexBatchJobDataGroup.addChild(error);
	}

	private void createAndAddUpdatedDataGroup(DataGroupSpy recordInfo) {
		DataGroupSpy updatedDataGroup = new DataGroupSpy("updated");
		updatedDataGroup.setRepeatId("0");
		updatedDataGroup.addChild(new DataAtomicSpy("tsUpdated", "2021-06-02T14:45:11.657482Z"));
		DataGroupSpy updatedBy = new DataGroupSpy("updatedBy", "user", "someUserId");
		updatedDataGroup.addChild(updatedBy);
		recordInfo.addChild(updatedDataGroup);
	}

	private IndexBatchJob createIndexBatchJob() {
		DataGroupSpy filter = new DataGroupSpy("filter");
		DataGroupSpy include = new DataGroupSpy("include");
		filter.addChild(include);
		IndexBatchJob indexBatchJob = new IndexBatchJob(SOME_RECORD_TYPE, 10, filter);
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
		DataGroup createdDataGroup = dataGroupHandler.createDataGroup(indexBatchJob);
		assertEquals(createdDataGroup.getNameInData(), "indexBatchJob");

		assertCorrectRecordInfo(createdDataGroup);
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("recordTypeToIndex"),
				SOME_RECORD_TYPE);
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("status"), "started");
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("numberOfProcessedRecords"),
				"0");
		assertEquals(createdDataGroup.getFirstAtomicValueWithNameInData("totalNumberToIndex"),
				"10");
		assertSame(createdDataGroup.getFirstGroupWithNameInData("filter"), indexBatchJob.filter);

		List<DataGroup> dataGroupErrors = createdDataGroup.getAllGroupsWithNameInData("error");

		assertCorrectSetErrorsInDataGroup(dataGroupErrors);
	}

	@Test
	public void testCreateDataGroupFromIndexBatchJobNoFilterIfFilterIsEmpty() throws Exception {
		DataGroupSpy emptyFilter = new DataGroupSpy("filter");
		indexBatchJob.filter = emptyFilter;

		DataGroup createdDataGroup = dataGroupHandler.createDataGroup(indexBatchJob);
		assertFalse(createdDataGroup.containsChildWithNameInData("filter"));
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
		DataGroup createdDataGroup = dataGroupHandler
				.createDataGroup(new IndexBatchJob("place", 10, new DataGroupSpy("filter")));
		assertEquals(createdDataGroup.getNameInData(), "indexBatchJob");
		assertFalse(createdDataGroup.containsChildWithNameInData("error"));

	}

}
