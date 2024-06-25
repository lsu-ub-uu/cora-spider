/*

 * Copyright 2022, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.password;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extended.systemsecret.SystemSecretOperations;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.spy.SystemSecretOperationsSpy;
import se.uu.ub.cora.spider.testspies.RecordCreatorSpy;
import se.uu.ub.cora.spider.testspies.RecordReaderSpy;
import se.uu.ub.cora.spider.testspies.RecordUpdaterSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class PasswordExtendedFunctionalityTest {
	private static final String SOME_PLAIN_TEXT_PASSWORD = "somePlainTextPassword";
	private static final String PASSWORD_LINK_NAME_IN_DATA = "passwordLink";
	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

	private SpiderDependencyProviderOldSpy dependencyProvider;
	private PasswordExtendedFunctionality extended;
	private ExtendedFunctionalityData efData;
	private DataFactorySpy dataFactory;
	private DataGroupSpy currentDataGroup;
	private MethodCallRecorder currentGroupMCR;
	private MethodReturnValues currentGroupMRV;
	private DataRecordLinkSpy dataDivider;
	private SpiderInstanceFactorySpy spiderInstanceFactory;
	private DateTimeFormatter dateTimePattern = DateTimeFormatter.ofPattern(DATE_PATTERN);
	private DataGroupSpy previousGroup;
	private SystemSecretOperationsSpy systemSecretOperations;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		dependencyProvider = new SpiderDependencyProviderOldSpy();
		systemSecretOperations = new SystemSecretOperationsSpy();

		extended = PasswordExtendedFunctionality.usingDependencyProviderAndSystemSecretOperations(
				dependencyProvider, systemSecretOperations);
		previousGroup = new DataGroupSpy();
		setupSpyForDataRecordGroup();
		createExtendedFunctionalityData();

		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);

		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.recordStorage;
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataGroupSpy::new);
	}

	private void setupSpyForDataRecordGroup() {
		currentDataGroup = new DataGroupSpy();
		currentGroupMCR = currentDataGroup.MCR;
		currentGroupMRV = currentDataGroup.MRV;
		setUpCurrentContainsPlainTextPasswordWithValue(true);
		setUpCurrentPasswordWithValue("true");
		setupDataDividerForDataRecordGroup();
	}

	private void setUpCurrentContainsPlainTextPasswordWithValue(
			boolean containsPlainTextPasswordValue) {
		currentGroupMRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> containsPlainTextPasswordValue, "plainTextPassword");
		currentGroupMRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> SOME_PLAIN_TEXT_PASSWORD, "plainTextPassword");
	}

	private void setUpPreviousUsePasswordWithValue(String usePassword) {
		previousGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> usePassword, "usePassword");
	}

	private void setUpCurrentPasswordWithValue(String usePasswordValue) {
		currentGroupMRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> usePasswordValue, "usePassword");
	}

	private void setupDataDividerForDataRecordGroup() {
		DataGroupSpy recordInfo = new DataGroupSpy();
		currentGroupMRV.setReturnValues("getFirstGroupWithNameInData", List.of(recordInfo),
				"recordInfo");
		dataDivider = new DataRecordLinkSpy();
		recordInfo.MRV.setReturnValues("getFirstChildWithNameInData", List.of(dataDivider),
				"dataDivider");
		dataDivider.MRV.setReturnValues("getLinkedRecordId", List.of("someDataDivider"));
	}

	private void createExtendedFunctionalityData() {
		efData = new ExtendedFunctionalityData();
		efData.dataGroup = currentDataGroup;
		efData.previouslyStoredTopDataGroup = previousGroup;
		efData.authToken = "fakeToken";
		efData.recordType = "fakeType";
		efData.recordId = "fakeId";
	}

	@Test
	public void testOnlyForTest() throws Exception {
		SpiderDependencyProvider returnedProvider = extended.onlyForTestGetDependencyProvider();
		assertEquals(returnedProvider, dependencyProvider);
		SystemSecretOperations systemSecretOperations = extended
				.onlyForTestGetSystemSecretOperations();
		assertEquals(systemSecretOperations, this.systemSecretOperations);
	}

	@Test
	public void testThrowDataExceptionIfUsePasswordTrueButNoPasswordSet() throws Exception {
		setUpPreviousUsePasswordWithValue("false");
		setUpCurrentPasswordWithValue("true");
		setUpCurrentContainsPlainTextPasswordWithValue(false);

		try {
			extended.useExtendedFunctionality(efData);
			Assert.fail();
		} catch (Exception e) {
			assertTrue(e instanceof DataException);
			assertEquals(e.getMessage(),
					"UsePassword set to true but no old password or new password exists.");
		}
	}

	@Test
	public void testUsePasswordTrueAndPreviousPasswordExistsDoNothing() throws Exception {
		setUpPreviousUsePasswordWithValue("true");
		setUpCurrentPasswordWithValue("true");
		setUpCurrentContainsPlainTextPasswordWithValue(false);

		extended.useExtendedFunctionality(efData);

		assertMinimalDataChange();
	}

	private void assertMinimalDataChange() {
		currentGroupMCR.assertNumberOfCallsToMethod("addChild", 0);
		currentGroupMCR.assertNumberOfCallsToMethod("removeAllChildrenWithNameInData", 1);
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"plainTextPassword");
	}

	@Test
	public void testPlainTextPasswordIsRemovedFromDataRecordGroupCreate() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		currentGroupMCR.assertCalledParameters("containsChildWithNameInData", "plainTextPassword");
		currentGroupMCR.assertCalledParameters("getFirstAtomicValueWithNameInData",
				"plainTextPassword");
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"plainTextPassword");
	}

	@Test
	public void testPlainTextPasswordIsRemovedFromDataRecordGroupUpdate() throws Exception {
		setUpSpiesForUpdateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		currentGroupMCR.assertCalledParameters("containsChildWithNameInData", "plainTextPassword");
		currentGroupMCR.assertCalledParameters("getFirstAtomicValueWithNameInData",
				"plainTextPassword");
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"plainTextPassword");
	}

	@Test
	public void testSystemSecretIsCreatedForNewPassword() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		systemSecretOperations.MCR.assertParameters("createAndStoreSystemSecretRecord", 0,
				SOME_PLAIN_TEXT_PASSWORD, "someDataDivider");
		String systemSecretId = (String) systemSecretOperations.MCR
				.getReturnValue("createAndStoreSystemSecretRecord", 0);

		assertLinkToSystemSecret(systemSecretId, 0);
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"plainTextPassword");
	}

	private void assertLinkToSystemSecret(String systemSecretId, int factorRecordLinkCallNumber) {
		dataFactory.MCR.assertCalledParameters("factorRecordLinkUsingNameInDataAndTypeAndId",
				PASSWORD_LINK_NAME_IN_DATA, SYSTEM_SECRET_TYPE, systemSecretId);

		var passwordLink = dataFactory.MCR.getReturnValue(
				"factorRecordLinkUsingNameInDataAndTypeAndId", factorRecordLinkCallNumber);

		currentGroupMCR.assertParameters("addChild", 0, passwordLink);
	}

	@Test
	public void testNewPasswordWithoutPreviousPassword() throws Exception {
		LocalDateTime dateTimeBefore = whatTimeIsIt().minus(2, ChronoUnit.SECONDS);

		extended.useExtendedFunctionality(efData);

		LocalDateTime dateTimeAfter = whatTimeIsIt().plus(2, ChronoUnit.SECONDS);

		assertTsUpdatedCreatedNowAndAddedToUserGroup(0, dateTimeBefore, dateTimeAfter);
		currentGroupMCR.assertNumberOfCallsToMethod("addChild", 2);
	}

	private void assertTsUpdatedCreatedNowAndAddedToUserGroup(int factoredAtomicCallNumber,
			LocalDateTime dateTimeBefore, LocalDateTime dateTimeAfter) {
		assertTimestampValue(factoredAtomicCallNumber, dateTimeBefore, dateTimeAfter);

		DataAtomicSpy tsUpdated = (DataAtomicSpy) dataFactory.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", "tsPasswordUpdated");
		currentGroupMCR.assertCalledParameters("addChild", tsUpdated);
	}

	private void assertTimestampValue(int factoredAtomicCallNumber, LocalDateTime dateTimeBefore,
			LocalDateTime dateTimeAfter) {
		String tsUpdatedValue = (String) dataFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", factoredAtomicCallNumber, "value");

		LocalDateTime updatedDateTime = LocalDateTime.parse(tsUpdatedValue, dateTimePattern);
		assertTrue(dateTimeBefore.isBefore(updatedDateTime));
		assertTrue(dateTimeAfter.isAfter(updatedDateTime));
	}

	private LocalDateTime whatTimeIsIt() {
		return LocalDateTime.now();
	}

	private String setUpSpiesForCreateReturningDataRecordWithTsUpdated() {
		RecordCreatorSpy recordCreatorSpy = new RecordCreatorSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordCreator", List.of(recordCreatorSpy));
		DataRecordSpy dataRecord = new DataRecordSpy();
		recordCreatorSpy.MRV.setDefaultReturnValuesSupplier("createAndStoreRecord",
				(Supplier<DataRecordSpy>) () -> dataRecord);

		DataGroupSpy recordGroup = new DataGroupSpy();
		dataRecord.MRV.setReturnValues("getDataGroup", List.of(recordGroup));

		DataGroupSpy recordInfo = new DataGroupSpy();
		recordGroup.MRV.setReturnValues("getFirstGroupWithNameInData", List.of(recordInfo),
				"recordInfo");
		DataGroupSpy updatedGroup = new DataGroupSpy();
		List<DataGroupSpy> updatedList = List.of(new DataGroupSpy(), updatedGroup);
		recordInfo.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(updatedList),
				"updated");

		String tsUpdated = "2018-11-29T13:55:55.827000Z";
		updatedGroup.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(tsUpdated),
				"tsUpdated");
		return tsUpdated;
	}

	@Test
	public void testUpdatePasswordExistingPassword() throws Exception {
		setUpSpiesForUpdateReturningDataRecordWithTsUpdated();

		LocalDateTime dateTimeBefore = whatTimeIsIt().minus(2, ChronoUnit.SECONDS);

		extended.useExtendedFunctionality(efData);

		LocalDateTime dateTimeAfter = whatTimeIsIt().plus(2, ChronoUnit.SECONDS);

		spiderInstanceFactory.MCR.assertMethodNotCalled("factorRecordCreator");

		DataRecordLinkSpy passwordLink = (DataRecordLinkSpy) currentDataGroup
				.getFirstChildOfTypeAndName(DataRecordLink.class, PASSWORD_LINK_NAME_IN_DATA);
		String systemSecretId = passwordLink.getLinkedRecordId();

		var plainTextPassword = currentGroupMCR.assertCalledParametersReturn(
				"getFirstAtomicValueWithNameInData", "plainTextPassword");
		systemSecretOperations.MCR.assertParameters("updateSecretForASystemSecret", 0,
				systemSecretId, "someDataDivider", plainTextPassword);

		currentDataGroup.MCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"tsPasswordUpdated");
		assertTsUpdatedCreatedNowAndAddedToUserGroup(0, dateTimeBefore, dateTimeAfter);
		currentGroupMCR.assertNumberOfCallsToMethod("addChild", 1);
	}

	private String setUpSpiesForUpdateReturningDataRecordWithTsUpdated() {
		currentGroupMRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"passwordLink");

		DataRecordLinkSpy passwordLink = new DataRecordLinkSpy();
		currentGroupMRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> passwordLink, DataRecordLink.class, PASSWORD_LINK_NAME_IN_DATA);

		passwordLink.MRV.setSpecificReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> "passwordLinkId");

		RecordReaderSpy recordReaderSpy = new RecordReaderSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordReader", List.of(recordReaderSpy));
		DataRecordSpy readDataRecord = new DataRecordSpy();
		recordReaderSpy.MRV.setDefaultReturnValuesSupplier("readRecord",
				(Supplier<DataRecordSpy>) () -> readDataRecord);

		DataGroupSpy readRecordGroup = new DataGroupSpy();
		readDataRecord.MRV.setReturnValues("getDataGroup", List.of(readRecordGroup));

		RecordUpdaterSpy recordUpdaterSpy = new RecordUpdaterSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordUpdater", List.of(recordUpdaterSpy));

		DataRecordSpy updatedDataRecord = new DataRecordSpy();
		recordUpdaterSpy.MRV.setDefaultReturnValuesSupplier("updateRecord",
				(Supplier<DataRecordSpy>) () -> updatedDataRecord);

		DataGroupSpy updatedRecordGroup = new DataGroupSpy();
		updatedDataRecord.MRV.setReturnValues("getDataGroup", List.of(updatedRecordGroup));

		DataGroupSpy updatedRecordInfo = new DataGroupSpy();
		updatedRecordGroup.MRV.setReturnValues("getFirstGroupWithNameInData",
				List.of(updatedRecordInfo), "recordInfo");

		DataGroupSpy updatedGroup = new DataGroupSpy();
		List<DataGroupSpy> updatedList = List.of(new DataGroupSpy(), updatedGroup);
		updatedRecordInfo.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(updatedList),
				"updated");

		String tsUpdated = "2022-04-23T13:55:55.827000Z";
		updatedGroup.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(tsUpdated),
				"tsUpdated");
		return tsUpdated;
	}

	@Test
	public void testRemovePassword_NoPasswordLinkFound() throws Exception {
		setUpCurrentPasswordWithValue("false");

		extended.useExtendedFunctionality(efData);

		currentGroupMCR.assertCalledParameters("getFirstAtomicValueWithNameInData", "usePassword");
		currentGroupMCR.assertCalledParameters("containsChildWithNameInData", "passwordLink");
		currentGroupMCR.assertNumberOfCallsToMethod("removeAllChildrenWithNameInData", 1);
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"plainTextPassword");
	}

	@Test
	public void testRemovePassword_PasswordLinkFound() throws Exception {
		setUpCurrentPasswordWithValue("false");
		currentGroupMRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"passwordLink");

		extended.useExtendedFunctionality(efData);

		currentGroupMCR.assertCalledParameters("getFirstAtomicValueWithNameInData", "usePassword");
		currentGroupMCR.assertCalledParameters("containsChildWithNameInData", "passwordLink");
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"tsPasswordUpdated");
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData", "passwordLink");
		currentGroupMCR.assertCalledParameters("removeAllChildrenWithNameInData",
				"plainTextPassword");
	}

}
