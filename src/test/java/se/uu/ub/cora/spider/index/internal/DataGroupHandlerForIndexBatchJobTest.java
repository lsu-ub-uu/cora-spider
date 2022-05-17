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

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;

public class DataGroupHandlerForIndexBatchJobTest {

	private static final String SOME_RECORD_TYPE = "someRecordType";
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";

	private DataAtomicFactorySpy atomicFactory;
	private IndexBatchJob indexBatchJob;
	private DataGroupOldSpy indexBatchJobDataGroup;
	private DataGroupHandlerForIndexBatchJobImp dataGroupHandler;
	private DataGroupFactorySpy dataGroupFactory;
	private DataFactorySpy dataFactorySpy;

	@BeforeMethod
	public void setUp() {
		setUpProviders();

		indexBatchJob = createIndexBatchJob();
		indexBatchJobDataGroup = createDataGroup();
		dataGroupHandler = new DataGroupHandlerForIndexBatchJobImp();
	}

	private void setUpProviders() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		atomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(atomicFactory);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
	}
	// TODO: reenable tests
	// @Test
	// public void testSetUpdateTimestampOnUpdate() {
	//
	// dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
	//
	// assertDataGroupFactoryFactoredCorrectly();
	//
	// DataGroupOldSpy addedUpdatedGroup = (DataGroupOldSpy) dataGroupFactory.MCR
	// .getReturnValue("factorUsingNameInData", 2);
	//
	// DataGroupOldSpy addedUpdatedBy = (DataGroupOldSpy) dataGroupFactory.MCR
	// .getReturnValue("factorAsLinkWithNameInDataTypeAndId", 0);
	//
	// DataChild addedChildToUpdated = addedUpdatedGroup.addedChildren.get(0);
	// assertSame(addedChildToUpdated, addedUpdatedBy);
	//
	// assertCorrectTsUpdated(addedUpdatedGroup);
	//
	// assertEquals(addedUpdatedGroup.repeatId, "1");
	// }
	//
	// private void assertCorrectTsUpdated(DataGroupOldSpy addedUpdatedGroup) {
	// DataAtomic tsUpdated = (DataAtomic) addedUpdatedGroup.addedChildren.get(1);
	// assertTrue(tsUpdated.getValue().matches(TIMESTAMP_FORMAT));
	//
	// DataGroup recordInfo = indexBatchJobDataGroup.getFirstGroupWithNameInData("recordInfo");
	// String tsCreated = recordInfo.getFirstAtomicValueWithNameInData("tsCreated");
	// assertFalse(tsUpdated.getValue().equals(tsCreated));
	// }
	//
	// private void assertDataGroupFactoryFactoredCorrectly() {
	// dataGroupFactory.MCR.assertParameter("factorUsingNameInData", 2, "nameInData", "updated");
	// dataGroupFactory.MCR.assertParameter("factorAsLinkWithNameInDataTypeAndId", 0, "nameInData",
	// "updatedBy");
	// dataGroupFactory.MCR.assertParameter("factorAsLinkWithNameInDataTypeAndId", 0, "recordType",
	// "user");
	// dataGroupFactory.MCR.assertParameter("factorAsLinkWithNameInDataTypeAndId", 0, "recordId",
	// "someSuperUser");
	// }
	//
	// @Test
	// public void testRepeatIdInUpdatedDataGroups() {
	// DataGroup recordInfo = indexBatchJobDataGroup.getFirstGroupWithNameInData("recordInfo");
	// addUpdatedToRecordInfo(recordInfo, "1");
	// addUpdatedToRecordInfo(recordInfo, "3");
	// addUpdatedToRecordInfo(recordInfo, "7");
	//
	// dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
	// DataGroupOldSpy addedUpdatedGroup = (DataGroupOldSpy) dataGroupFactory.MCR
	// .getReturnValue("factorUsingNameInData", 2);
	// assertEquals(addedUpdatedGroup.repeatId, "4");
	//
	// assertRepeatIdsWereReplacedWhenNewGroupWasAdded(recordInfo);
	//
	// }
	//
	// private void assertRepeatIdsWereReplacedWhenNewGroupWasAdded(DataGroup recordInfo) {
	// List<DataGroup> updatedGroups = recordInfo.getAllGroupsWithNameInData("updated");
	// assertEquals(updatedGroups.get(0).getRepeatId(), "0");
	// assertEquals(updatedGroups.get(1).getRepeatId(), "1");
	// assertEquals(updatedGroups.get(2).getRepeatId(), "2");
	// assertEquals(updatedGroups.get(3).getRepeatId(), "3");
	// assertEquals(updatedGroups.get(4).getRepeatId(), "4");
	// }
	//
	// private void addUpdatedToRecordInfo(DataGroup recordInfo, String repeatId) {
	// DataGroupOldSpy updated = new DataGroupOldSpy("updated");
	// updated.setRepeatId(repeatId);
	// recordInfo.addChild(updated);
	// }
	//
	// @Test
	// public void testUpdateNumOfProcessedRecordsInDataGroup() {
	// indexBatchJob.numberOfProcessedRecords = 67;
	//
	// dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
	// assertEquals(indexBatchJobDataGroup.removedNameInDatas.get(0), "numberOfProcessedRecords");
	// assertEquals(atomicFactory.nameInDatas.get(0), "numberOfProcessedRecords");
	// assertEquals(indexBatchJobDataGroup
	// .getFirstAtomicValueWithNameInData("numberOfProcessedRecords"), "67");
	// }
	//
	// @Test
	// public void testUpdateErrorsinDataGroup() {
	// dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
	// List<DataGroup> errors = indexBatchJobDataGroup.getAllGroupsWithNameInData("error");
	// assertEquals(errors.size(), 3);
	//
	// assertCorrectError(0, "0", "someRecordId", "some read error message");
	// assertCorrectError(1, "1", "recordIdOne", "some index error message");
	// assertCorrectError(2, "2", "recordIdTwo", "some other index error message");
	// }
	//
	// @Test
	// public void testStatusStartedDoesNotOverwriteStorage() {
	// indexBatchJob.status = "started";
	// dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
	//
	// assertStatusInUpdatedDataGroupEquals("someStatus");
	// }
	//
	// @Test
	// public void testStatusPausedDoesNotOverwriteStorage() {
	// indexBatchJob.status = "paused";
	// dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
	//
	// assertStatusInUpdatedDataGroupEquals("someStatus");
	// }
	//
	// @Test
	// public void testStatusFinishedDoesOverwriteStorage() {
	// indexBatchJob.status = "finished";
	// dataGroupHandler.updateDataGroup(indexBatchJob, indexBatchJobDataGroup);
	//
	// assertStatusInUpdatedDataGroupEquals("finished");
	// }
	//
	// private void assertStatusInUpdatedDataGroupEquals(String expected) {
	// assertEquals(indexBatchJobDataGroup.getFirstAtomicValueWithNameInData("status"), expected);
	// }
	//
	// private void assertCorrectError(int index, String repeatId, String recordId, String message)
	// {
	// List<DataGroup> errors = indexBatchJobDataGroup.getAllGroupsWithNameInData("error");
	// DataGroup error = errors.get(index);
	// assertEquals(error.getRepeatId(), repeatId);
	// assertEquals(error.getFirstAtomicValueWithNameInData("recordId"), recordId);
	// assertEquals(error.getFirstAtomicValueWithNameInData("message"), message);
	// }

	private DataGroupOldSpy createDataGroup() {
		DataGroupOldSpy indexBatchJobDataGroup = new DataGroupOldSpy("indexBatchJob");
		indexBatchJobDataGroup.addChild(new DataAtomicSpy("numOfProcessedRecords", "34"));
		indexBatchJobDataGroup.addChild(new DataAtomicSpy("totalNumberToIndex", "34"));
		indexBatchJobDataGroup.addChild(new DataAtomicSpy("status", "someStatus"));
		createAndAddErrorDataGroup(indexBatchJobDataGroup);
		DataGroupOldSpy recordInfo = createRecordInfo();
		indexBatchJobDataGroup.addChild(recordInfo);
		return indexBatchJobDataGroup;
	}

	private DataGroupOldSpy createRecordInfo() {
		DataGroupOldSpy recordInfo = new DataGroupOldSpy("recordInfo");
		DataGroupOldSpy createdBy = new DataGroupOldSpy("createdBy", "user", "someSuperUser");
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicSpy("tsCreated", "2021-05-03T14:12:11.657482Z"));
		createAndAddUpdatedDataGroup(recordInfo);
		return recordInfo;
	}

	private void createAndAddErrorDataGroup(DataGroupOldSpy indexBatchJobDataGroup) {
		DataGroupOldSpy error = new DataGroupOldSpy("error");
		error.setRepeatId("0");
		error.addChild(new DataAtomicSpy("recordId", "someRecordId"));
		error.addChild(new DataAtomicSpy("message", "some read error message"));
		indexBatchJobDataGroup.addChild(error);
	}

	private void createAndAddUpdatedDataGroup(DataGroupOldSpy recordInfo) {
		DataGroupOldSpy updatedDataGroup = new DataGroupOldSpy("updated");
		updatedDataGroup.setRepeatId("0");
		updatedDataGroup.addChild(new DataAtomicSpy("tsUpdated", "2021-06-02T14:45:11.657482Z"));
		DataGroupOldSpy updatedBy = new DataGroupOldSpy("updatedBy", "user", "someUserId");
		updatedDataGroup.addChild(updatedBy);
		recordInfo.addChild(updatedDataGroup);
	}

	private IndexBatchJob createIndexBatchJob() {
		DataGroupOldSpy filter = new DataGroupOldSpy("filter");
		DataGroupOldSpy include = new DataGroupOldSpy("include");
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
		DataGroupSpy createdGroup = (DataGroupSpy) dataGroupHandler.createDataGroup(indexBatchJob);

		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 0, "indexBatchJob");
		dataFactorySpy.MCR.assertReturn("factorGroupUsingNameInData", 0, createdGroup);

		assertCorrectRecordInfo(createdGroup);

		assertFactoredAtomicNoWithNameAndValueAddedAsNo(createdGroup, 0, "recordTypeToIndex",
				indexBatchJob.recordTypeToIndex, 1);
		assertFactoredAtomicNoWithNameAndValueAddedAsNo(createdGroup, 1, "status", "started", 2);
		assertFactoredAtomicNoWithNameAndValueAddedAsNo(createdGroup, 2, "numberOfProcessedRecords",
				"0", 3);
		assertFactoredAtomicNoWithNameAndValueAddedAsNo(createdGroup, 3, "totalNumberToIndex", "10",
				4);

		createdGroup.MCR.assertParameters("addChild", 5, indexBatchJob.filter);

		assertFactoredErrorsNoAddedAsNoFactoredAtomic(createdGroup, indexBatchJob.errors, 2, 6, 4);
		createdGroup.MCR.assertNumberOfCallsToMethod("addChild", 8);
	}

	private void assertFactoredAtomicNoWithNameAndValueAddedAsNo(DataGroupSpy createdDataGroup,
			int factoredNo, String name, String value, int addedNo) {
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", factoredNo, name,
				value);
		var recordTypeToIndex = dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", factoredNo);
		createdDataGroup.MCR.assertParameters("addChild", addedNo, recordTypeToIndex);
	}

	private void assertFactoredErrorsNoAddedAsNoFactoredAtomic(DataGroupSpy createdGroup,
			List<IndexError> errorList, int factoredNo, int addedNo, int factoredAtomic) {
		List<IndexError> errors = errorList;
		int c = 0;
		int c2 = 0;
		for (IndexError indexError : errors) {

			dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", factoredNo + c,
					"error");
			DataGroupSpy error = (DataGroupSpy) dataFactorySpy.MCR
					.getReturnValue("factorGroupUsingNameInData", factoredNo + c);
			error.MCR.assertParameters("setRepeatId", 0, String.valueOf(c));
			createdGroup.MCR.assertParameters("addChild", addedNo + c, error);

			assertFactoredAtomicNoWithNameAndValueAddedAsNo(error, factoredAtomic + c2, "recordId",
					indexError.recordId, 0);
			assertFactoredAtomicNoWithNameAndValueAddedAsNo(error, factoredAtomic + c2 + 1,
					"message", indexError.message, 1);
			c++;
			c2++;
			c2++;
		}
	}

	@Test
	public void testCreateDataGroupFromIndexBatchJobNoFilterIfFilterIsEmpty() throws Exception {
		DataGroupOldSpy emptyFilter = new DataGroupOldSpy("filter");
		indexBatchJob.filter = emptyFilter;

		DataGroupSpy createdGroup = (DataGroupSpy) dataGroupHandler.createDataGroup(indexBatchJob);
		// assertFalse(createdDataGroup.containsChildWithNameInData("filter"));
		createdGroup.MCR.assertNumberOfCallsToMethod("addChild", 7);
	}

	private void assertCorrectRecordInfo(DataGroupSpy createdDataGroup) {
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 1, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 1);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"dataDivider", "system", "cora");
		var dataDivider = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		recordInfo.MCR.assertParameters("addChild", 0, dataDivider);

		createdDataGroup.MCR.assertParameters("addChild", 0, recordInfo);
	}

	// private void assertCorrectSetErrorsInDataGroup(List<DataGroup> dataGroupErrors) {
	// List<IndexError> indexErrors = indexBatchJob.errors;
	// assertCorrectErrorInDataGroup(dataGroupErrors, indexErrors, 0);
	// assertCorrectErrorInDataGroup(dataGroupErrors, indexErrors, 1);
	//
	// assertEquals(dataGroupErrors.size(), 2);
	// }

	// private void assertCorrectErrorInDataGroup(List<DataGroup> dataGroupErrors,
	// List<IndexError> indexErrors, int index) {
	// DataGroup error = dataGroupErrors.get(index);
	// assertEquals(error.getFirstAtomicValueWithNameInData("recordId"),
	// indexErrors.get(index).recordId);
	// assertEquals(error.getFirstAtomicValueWithNameInData("message"),
	// indexErrors.get(index).message);
	// assertEquals(error.getRepeatId(), String.valueOf(index));
	// }

	@Test
	public void testCreateDataGroupFromIndexBatchJobEmptyErrors() {
		DataGroupSpy createdGroup = (DataGroupSpy) dataGroupHandler
				.createDataGroup(new IndexBatchJob("place", 10, new DataGroupOldSpy("filter")));
		// assertEquals(createdGroup.getNameInData(), "indexBatchJob");
		// assertFalse(createdGroup.containsChildWithNameInData("error"));

		createdGroup.MCR.assertNumberOfCallsToMethod("addChild", 5);

	}

}
