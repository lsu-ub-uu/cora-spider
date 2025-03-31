/*
 * Copyright 2024, 2025 Uppsala University Library
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

package se.uu.ub.cora.spider.systemsecret;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordIdGeneratorSpy;
import se.uu.ub.cora.spider.extended.password.TextHasherSpy;
import se.uu.ub.cora.spider.spy.DataChangedSenderSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class SystemSecretOperationsTest {

	private RecordIdGeneratorSpy recordIdGenerator;
	private SystemSecretOperationsImp systemSecretOperations;
	private DataFactorySpy dataFactory;
	private SpiderDependencyProviderSpy dependencyProvider;

	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private static final String SOME_SYSTEM_SECRET_ID = "someSystemSecretId";
	private static final String SYSTEM_SECRET_VALIDATION_TYPE = "systemSecret";
	private static final String SYSTEM_SECRET_NAME_IN_DATA = "systemSecret";
	private static final String SOME_DATA_DIVIDER = "someDataDivider";
	private static final String SECRET = "secret";
	private TextHasherSpy textHasher;
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		textHasher = new TextHasherSpy();
		recordStorage = new RecordStorageSpy();
		recordIdGenerator = new RecordIdGeneratorSpy();
		createAndSetupDependencyProvider();

		systemSecretOperations = SystemSecretOperationsImp
				.usingDependencyProviderAndTextHasher(dependencyProvider, textHasher);
	}

	private void createAndSetupDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordIdGenerator",
				() -> recordIdGenerator);
	}

	@Test
	public void testImplementsSystemSecretCreator() {
		assertTrue(systemSecretOperations instanceof SystemSecretOperations);
	}

	@Test
	public void testCreateNewSystemSecretRecord() {
		String createdSystemSecretId = systemSecretOperations
				.createAndStoreSystemSecretRecord("someSystemSecretValue", SOME_DATA_DIVIDER);

		textHasher.MCR.assertParameters("hashText", 0, "someSystemSecretValue");

		recordIdGenerator.MCR.assertParameters("getIdForType", 0, SYSTEM_SECRET_TYPE);
		String systemSecretId = (String) recordIdGenerator.MCR
				.assertCalledParametersReturn("getIdForType", SYSTEM_SECRET_TYPE);

		DataRecordGroupSpy systemSecretRecordGroup = assertAndReturnSystemSecretDataGroup(
				systemSecretId);

		var systemSecretGroup = dataFactory.MCR.assertCalledParametersReturn(
				"factorGroupFromDataRecordGroup", systemSecretRecordGroup);

		recordStorage.MCR.assertParameters("create", 0, SYSTEM_SECRET_TYPE, systemSecretId,
				systemSecretGroup, Collections.emptySet(), Collections.emptySet(),
				SOME_DATA_DIVIDER);
		assertEquals(createdSystemSecretId, systemSecretId);
	}

	@Test
	public void testCreateNewSendDataChanged() {
		String createdSystemSecretId = systemSecretOperations
				.createAndStoreSystemSecretRecord("someSystemSecretValue", SOME_DATA_DIVIDER);

		var sender = getDataChangedSender();
		sender.MCR.assertParameters("sendDataChanged", 0, SYSTEM_SECRET_TYPE, createdSystemSecretId,
				"create");
	}

	private DataChangedSenderSpy getDataChangedSender() {
		return (DataChangedSenderSpy) dependencyProvider.MCR
				.assertCalledParametersReturn("getDataChangeSender");
	}

	@Test
	public void testSendDataChangedAfterCreateInStorage() {
		recordStorage.MRV.setAlwaysThrowException("create", new RuntimeException("someError"));

		try {
			systemSecretOperations.createAndStoreSystemSecretRecord("someSystemSecretValue",
					SOME_DATA_DIVIDER);
			fail();
		} catch (Exception e) {
			dependencyProvider.MCR.assertMethodNotCalled("getDataChangeSender");
		}
	}

	private DataRecordGroupSpy assertAndReturnSystemSecretDataGroup(String systemSecretId) {
		DataRecordGroupSpy systemSecretRecordGroup = (DataRecordGroupSpy) dataFactory.MCR
				.assertCalledParametersReturn("factorRecordGroupUsingNameInData",
						SYSTEM_SECRET_NAME_IN_DATA);

		systemSecretRecordGroup.MCR.assertParameters("setType", 0, SYSTEM_SECRET_TYPE);
		systemSecretRecordGroup.MCR.assertParameters("setId", 0, systemSecretId);
		systemSecretRecordGroup.MCR.assertParameters("setDataDivider", 0, SOME_DATA_DIVIDER);
		systemSecretRecordGroup.MCR.assertParameters("setValidationType", 0,
				SYSTEM_SECRET_VALIDATION_TYPE);

		assertHashedSecretIsAddedToGroupAsNumber(systemSecretRecordGroup);
		return systemSecretRecordGroup;
	}

	private void assertHashedSecretIsAddedToGroupAsNumber(DataRecordGroupSpy systemSecretGroup) {
		var hashedPassword = textHasher.MCR.getReturnValue("hashText", 0);
		var hashedPasswordAtomic = dataFactory.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", SECRET, hashedPassword);
		systemSecretGroup.MCR.assertCalledParameters("addChild", hashedPasswordAtomic);
	}

	@Test
	public void testUpdateSecretInSystemSecret() {
		DataRecordGroupSpy existingSystemSecret = new DataRecordGroupSpy();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> existingSystemSecret);

		systemSecretOperations.updateSecretForASystemSecret(SOME_SYSTEM_SECRET_ID,
				SOME_DATA_DIVIDER, "someSecret");

		recordStorage.MCR.assertParameters("read", 0, SYSTEM_SECRET_TYPE, SOME_SYSTEM_SECRET_ID);
		existingSystemSecret.MCR.assertParameters("removeAllChildrenWithNameInData", 0, SECRET);

		assertHashedSecretIsAddedToGroupAsNumber(existingSystemSecret);

		DataGroup existingSystemSecretTopDataGroup = (DataGroup) dataFactory.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup",
						existingSystemSecret);

		recordStorage.MCR.assertParameters("update", 0, SYSTEM_SECRET_TYPE, SOME_SYSTEM_SECRET_ID,
				existingSystemSecretTopDataGroup, Collections.emptySet(), Collections.emptySet(),
				SOME_DATA_DIVIDER);
	}

	@Test
	public void testUpdateSendDataChanged() {
		systemSecretOperations.updateSecretForASystemSecret(SOME_SYSTEM_SECRET_ID,
				SOME_DATA_DIVIDER, "someSecret");

		var sender = getDataChangedSender();
		sender.MCR.assertParameters("sendDataChanged", 0, SYSTEM_SECRET_TYPE, SOME_SYSTEM_SECRET_ID,
				"update");
	}

	@Test
	public void testSendDataChangedAfterUpdateInStorage() {
		recordStorage.MRV.setAlwaysThrowException("update", new RuntimeException("someError"));

		try {
			systemSecretOperations.updateSecretForASystemSecret(SOME_SYSTEM_SECRET_ID,
					SOME_DATA_DIVIDER, "someSecret");
			fail();
		} catch (Exception e) {
			dependencyProvider.MCR.assertMethodNotCalled("getDataChangeSender");
		}
	}

	@Test
	public void testOnlyForTest() {
		assertEquals(systemSecretOperations.onlyForTestGetDependencyProvider(), dependencyProvider);
		assertEquals(systemSecretOperations.onlyForTestGetTextHasher(), textHasher);
	}

	@Test
	public void testRemoveSystemSecretFromStorage() {
		systemSecretOperations.deleteSystemSecretFromStorage(SOME_SYSTEM_SECRET_ID);

		recordStorage.MCR.assertCalledParameters("deleteByTypeAndId", SYSTEM_SECRET_TYPE,
				SOME_SYSTEM_SECRET_ID);
	}

	@Test
	public void testDeleteSendDataChanged() {
		systemSecretOperations.deleteSystemSecretFromStorage(SOME_SYSTEM_SECRET_ID);

		var sender = getDataChangedSender();
		sender.MCR.assertParameters("sendDataChanged", 0, SYSTEM_SECRET_TYPE, SOME_SYSTEM_SECRET_ID,
				"delete");
	}

	@Test
	public void testSendDataChangedAfterDeleteInStorage() {
		recordStorage.MRV.setAlwaysThrowException("deleteByTypeAndId",
				new RuntimeException("someError"));

		try {
			systemSecretOperations.deleteSystemSecretFromStorage(SOME_SYSTEM_SECRET_ID);
			fail();
		} catch (Exception e) {
			dependencyProvider.MCR.assertMethodNotCalled("getDataChangeSender");
		}
	}
}
