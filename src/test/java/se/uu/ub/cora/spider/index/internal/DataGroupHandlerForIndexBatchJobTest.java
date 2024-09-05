/*
 * Copyright 2021, 2023 Uppsala University Library
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

import static org.testng.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.storage.Filter;

public class DataGroupHandlerForIndexBatchJobTest {

	private static final String SOME_RECORD_TYPE = "someRecordType";

	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern(DATE_TIME_PATTERN);

	private IndexBatchJob indexBatchJob;
	private DataGroupHandlerForIndexBatchJobImp dataGroupHandler;
	private DataFactorySpy dataFactorySpy;
	/**
	 * numberOfFactoredAtomics
	 */
	int nfa = 0;
	/**
	 * numberOfFactoredRecordLinks
	 */
	int nfrl = 0;
	/**
	 * numberOfFactoredGroups
	 */
	int nfg = 0;

	@BeforeMethod
	public void beforeMethod() {
		nfa = 0;
		nfrl = 0;
		nfg = 0;
		setUpProviders();

		indexBatchJob = createIndexBatchJob();
		dataGroupHandler = new DataGroupHandlerForIndexBatchJobImp();
	}

	private void setUpProviders() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	String dataGroupLayout = """
			indexBatchJob
				recordInfo
					createdBy user someSuperUser
					tsCreated 2021-05-03T14:12:11.657482Z
					updated
						tsUpdated 2021-06-02T14:45:11.657482Z
						updatedBy user someUserId
				numOfProcessedRecords 34
				totalNumberToIndex 34
				status someStatus
				error
					recordId someRecordId
					message some read error message
				""";

	Integer callsToAddChildForCheckingIndexBatchJob = 0;
	Integer callsToRemoveFirstChildWithNameForIndexBatchJob = 0;

	@Test
	public void testUpdateDataGroupWithInfoFromIndexBatchJobForUpdate() {
		callsToAddChildForCheckingIndexBatchJob = 0;
		callsToRemoveFirstChildWithNameForIndexBatchJob = 0;
		DataGroupSpy existingIndexBatchJob = new DataGroupSpy();
		// setListOfErrors
		List<DataGroupSpy> listOfErrorsSpies = List.of(new DataGroupSpy(), new DataGroupSpy());
		int firstErrorRepeatId = listOfErrorsSpies.size() + 1;
		existingIndexBatchJob.MRV.setReturnValues("getAllGroupsWithNameInData",
				List.of(listOfErrorsSpies), "error");

		// recordInfoSpy
		DataGroupSpy recordInfoG = new DataGroupSpy();
		existingIndexBatchJob.MRV.setReturnValues("getFirstGroupWithNameInData",
				List.of(recordInfoG), "recordInfo");
		// setListOfUpdated
		List<DataGroupSpy> listOfUpdatedSpies = List.of(new DataGroupSpy());
		int firstUpdatedRepeatId = listOfUpdatedSpies.size() + 1;
		recordInfoG.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(listOfUpdatedSpies),
				"updated");

		LocalDateTime dateTimeBefore = whatTimeIsIt().minus(2, ChronoUnit.SECONDS);
		dataGroupHandler.updateDataGroup(indexBatchJob, existingIndexBatchJob);
		LocalDateTime dateTimeAfter = whatTimeIsIt().plus(2, ChronoUnit.SECONDS);

		assertNumberOfProcessedRecords(existingIndexBatchJob);
		assertErrorsFromBatchJobAddedToGroupAndFirstRepeatId(existingIndexBatchJob,
				firstErrorRepeatId);
		assertStatus(existingIndexBatchJob);
		assertNewUpdatedAddedToRecordInfo(existingIndexBatchJob, firstUpdatedRepeatId,
				dateTimeBefore, dateTimeAfter);
	}

	private void assertNewUpdatedAddedToRecordInfo(DataGroupSpy existingIndexBatchJob,
			int firstUpdatedRepeatId, LocalDateTime dateTimeBefore, LocalDateTime dateTimeAfter) {
		existingIndexBatchJob.MCR.assertParameters("getFirstGroupWithNameInData", 0, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) existingIndexBatchJob.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);

		recordInfo.MCR.assertParameters("getAllGroupsWithNameInData", 0, "updated");

		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", nfg, "updated");
		DataGroupSpy updated = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", nfg);
		recordInfo.MCR.assertParameters("addChild", 0, updated);
		updated.MCR.assertParameters("setRepeatId", 0, String.valueOf(firstUpdatedRepeatId));
		callsToAddChildForCheckingIndexBatchJob++;
		nfg++;

		assertUpdateBy(updated);
		assertTsUpdated(dateTimeBefore, dateTimeAfter, updated);
	}

	private void assertUpdateBy(DataGroupSpy updated) {
		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", nfrl,
				"updatedBy", "user", "system");
		var updatedBy = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", nfrl);
		updated.MCR.assertParameters("addChild", 0, updatedBy);
		nfrl++;
	}

	private void assertTsUpdated(LocalDateTime dateTimeBefore, LocalDateTime dateTimeAfter,
			DataGroupSpy updated) {
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", nfa,
				"tsUpdated");
		DataAtomicOldSpy tsUpdated = (DataAtomicOldSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", nfa);
		String tsUpdatedValue = (String) dataFactorySpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", nfa, "value");
		nfa++;

		LocalDateTime tsUpdatedDateTime = LocalDateTime.parse(tsUpdatedValue, dateTimeFormatter);
		assertTrue(dateTimeBefore.isBefore(tsUpdatedDateTime));
		assertTrue(dateTimeAfter.isAfter(tsUpdatedDateTime));

		updated.MCR.assertParameters("addChild", 1, tsUpdated);
	}

	private LocalDateTime whatTimeIsIt() {
		return LocalDateTime.now();
	}

	private void assertNumberOfProcessedRecords(DataGroupSpy existingIndexBatchJob) {
		existingIndexBatchJob.MCR.assertParameters("removeFirstChildWithNameInData",
				callsToRemoveFirstChildWithNameForIndexBatchJob, "numberOfProcessedRecords");
		callsToRemoveFirstChildWithNameForIndexBatchJob++;

		assertFactoredAtomicWithNameAndValueAddedToAsNo("numberOfProcessedRecords",
				String.valueOf(indexBatchJob.numberOfProcessedRecords), existingIndexBatchJob,
				callsToAddChildForCheckingIndexBatchJob);
		callsToAddChildForCheckingIndexBatchJob++;
	}

	private void assertStatus(DataGroupSpy existingIndexBatchJob) {
		existingIndexBatchJob.MCR.assertParameters("removeFirstChildWithNameInData",
				callsToRemoveFirstChildWithNameForIndexBatchJob, "status");
		callsToRemoveFirstChildWithNameForIndexBatchJob++;

		assertFactoredAtomicWithNameAndValueAddedToAsNo("status",
				String.valueOf(indexBatchJob.status), existingIndexBatchJob,
				callsToAddChildForCheckingIndexBatchJob);
		callsToAddChildForCheckingIndexBatchJob++;

	}

	private IndexBatchJob createIndexBatchJob() {
		Filter filter = new Filter();

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
	public void testConvertIndexBatchJobToDataGroupForCreate() {
		DataGroupSpy filterAsDataGroup = new DataGroupSpy();
		DataGroupSpy createdGroup = (DataGroupSpy) dataGroupHandler.createDataGroup(indexBatchJob,
				filterAsDataGroup);

		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", nfg, "indexBatchJob");
		dataFactorySpy.MCR.assertReturn("factorGroupUsingNameInData", nfg, createdGroup);
		nfg++;

		assertCorrectRecordInfo(createdGroup);

		assertFactoredAtomicWithNameAndValueAddedToAsNo("recordTypeToIndex",
				indexBatchJob.recordTypeToIndex, createdGroup,
				callsToAddChildForCheckingIndexBatchJob);
		callsToAddChildForCheckingIndexBatchJob++;
		assertFactoredAtomicWithNameAndValueAddedToAsNo("status", "started", createdGroup,
				callsToAddChildForCheckingIndexBatchJob);
		callsToAddChildForCheckingIndexBatchJob++;
		assertFactoredAtomicWithNameAndValueAddedToAsNo("numberOfProcessedRecords", "0",
				createdGroup, callsToAddChildForCheckingIndexBatchJob);
		callsToAddChildForCheckingIndexBatchJob++;
		assertFactoredAtomicWithNameAndValueAddedToAsNo("totalNumberToIndex", "10", createdGroup,
				callsToAddChildForCheckingIndexBatchJob);
		callsToAddChildForCheckingIndexBatchJob++;
		createdGroup.MCR.assertParameters("addChild", callsToAddChildForCheckingIndexBatchJob,
				filterAsDataGroup);
		callsToAddChildForCheckingIndexBatchJob++;

		assertErrorsFromBatchJobAddedToGroupAndFirstRepeatId(createdGroup, 1);
		createdGroup.MCR.assertNumberOfCallsToMethod("addChild",
				callsToAddChildForCheckingIndexBatchJob);
	}

	private void assertCorrectRecordInfo(DataGroupSpy createdDataGroup) {
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", nfg, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", nfg);
		nfg++;

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", nfrl,
				"dataDivider", "system", "cora");
		var dataDivider = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", nfrl);
		nfrl++;
		recordInfo.MCR.assertParameters("addChild", 0, dataDivider);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", nfrl,
				"validationType", "validationType", "indexBatchJob");
		var validationType = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", nfrl);
		nfrl++;
		recordInfo.MCR.assertParameters("addChild", 1, validationType);

		createdDataGroup.MCR.assertParameters("addChild", callsToAddChildForCheckingIndexBatchJob,
				recordInfo);
		callsToAddChildForCheckingIndexBatchJob++;
	}

	private void assertFactoredAtomicWithNameAndValueAddedToAsNo(String name, String value,
			DataGroupSpy createdDataGroup, int addedNo) {
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", nfa, name,
				value);
		DataAtomicOldSpy factoredAtomic = (DataAtomicOldSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", nfa);
		createdDataGroup.MCR.assertParameters("addChild", addedNo, factoredAtomic);
		nfa++;
	}

	private void assertErrorsFromBatchJobAddedToGroupAndFirstRepeatId(DataGroupSpy addedToGroup,
			int startRepeatId) {
		int repeatId = startRepeatId;
		for (IndexError indexError : indexBatchJob.errors) {
			assertFactoredGroupWithNameAndRepeatIdAddedToAsNo("error", String.valueOf(repeatId),
					addedToGroup, callsToAddChildForCheckingIndexBatchJob);
			callsToAddChildForCheckingIndexBatchJob++;

			DataGroupSpy errorGroup = (DataGroupSpy) dataFactorySpy.MCR
					.getReturnValue("factorGroupUsingNameInData", nfg);
			nfg++;

			assertFactoredAtomicWithNameAndValueAddedToAsNo("recordId", indexError.recordId,
					errorGroup, 0);
			assertFactoredAtomicWithNameAndValueAddedToAsNo("message", indexError.message,
					errorGroup, 1);
			repeatId++;
		}
	}

	private void assertFactoredGroupWithNameAndRepeatIdAddedToAsNo(String name, String repeatId,
			DataGroupSpy createdGroup, int addedNo) {
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", nfg, name);
		DataGroupSpy factoredGroup = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", nfg);
		if (!"".equals(repeatId)) {
			factoredGroup.MCR.assertParameters("setRepeatId", 0, repeatId);
		}
		createdGroup.MCR.assertParameters("addChild", addedNo, factoredGroup);
	}

	@Test
	public void testCreateDataGroupFromIndexBatchJobNoFilterIfFilterIsEmpty() throws Exception {
		DataGroupOldSpy filterAsData = new DataGroupOldSpy("filter");

		DataGroupSpy createdGroup = (DataGroupSpy) dataGroupHandler.createDataGroup(indexBatchJob,
				filterAsData);
		createdGroup.MCR.assertNumberOfCallsToMethod("addChild", 7);
	}

	@Test
	public void testCreateDataGroupFromIndexBatchJobEmptyErrors() {
		IndexBatchJob indexBatchJob = new IndexBatchJob("place", 10, new Filter());
		DataGroupSpy createdGroup = (DataGroupSpy) dataGroupHandler.createDataGroup(indexBatchJob,
				new DataGroupOldSpy("filter"));

		createdGroup.MCR.assertNumberOfCallsToMethod("addChild", 5);

	}

}
