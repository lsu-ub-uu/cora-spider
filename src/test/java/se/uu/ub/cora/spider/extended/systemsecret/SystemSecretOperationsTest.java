/*
 * Copyright 2024 Uppsala University Library
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

package se.uu.ub.cora.spider.extended.systemsecret;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordIdGeneratorSpy;
import se.uu.ub.cora.spider.extended.password.TextHasherSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class SystemSecretOperationsTest {

	private RecordIdGeneratorSpy recordIdGenerator;
	private SystemSecretOperationsImp systemSecretOperations;
	private DataFactorySpy dataFactory;
	private SpiderDependencyProviderSpy dependencyProvider;

	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private static final String RECORD_INFO = "recordInfo";
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

		systemSecretOperations = new SystemSecretOperationsImp(dependencyProvider, textHasher);

	}

	private void createAndSetupDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordIdGenerator",
				() -> recordIdGenerator);
	}

	@Test
	public void testImplementsSystemSecretCreator() throws Exception {
		assertTrue(systemSecretOperations instanceof SystemSecretOperations);
	}

	@Test
	public void testCreateNewSystemSecretRecord() throws Exception {

		String createdSystemSecretId = systemSecretOperations
				.createAndStoreSystemSecretRecord("someSystemSecretValue", "someDataDivider");

		textHasher.MCR.assertParameters("hashText", 0, "someSystemSecretValue");

		recordIdGenerator.MCR.assertParameters("getIdForType", 0, SYSTEM_SECRET_TYPE);
		String systemSecretId = (String) recordIdGenerator.MCR
				.assertCalledParametersReturn("getIdForType", SYSTEM_SECRET_TYPE);

		DataGroupSpy systemSecretDataGroup = assertAndReturnSystemSecretDataGroup(systemSecretId);

		recordStorage.MCR.assertParameters("create", 0, SYSTEM_SECRET_TYPE, systemSecretId,
				systemSecretDataGroup, Collections.emptySet(), Collections.emptySet(),
				"someDataDivider");
		assertEquals(createdSystemSecretId, systemSecretId);

	}

	private DataGroupSpy assertAndReturnSystemSecretDataGroup(String systemSecretId) {
		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 0, SYSTEM_SECRET_TYPE);
		DataGroupSpy systemSecretDataGroup = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		//
		assertRecordInfoWithDataDividerAddedTo(systemSecretDataGroup, systemSecretId);
		assertHashedSecretIsAddedToGroupAsNumber(systemSecretDataGroup);
		return systemSecretDataGroup;
	}

	private void assertRecordInfoWithDataDividerAddedTo(DataGroupSpy systemSecret,
			String systemSecretId) {
		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 1, RECORD_INFO);
		DataGroupSpy recordInfo = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 1);
		systemSecret.MCR.assertParameters("addChild", 0, recordInfo);

		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0, "type",
				"recordType", SYSTEM_SECRET_TYPE);
		var typeLink = dataFactory.MCR.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId",
				0);
		recordInfo.MCR.assertParameters("addChild", 0, typeLink);

		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "id",
				systemSecretId);
		var id = dataFactory.MCR.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);
		recordInfo.MCR.assertParameters("addChild", 1, id);
	}

	private void assertHashedSecretIsAddedToGroupAsNumber(DataGroupSpy systemSecretGroup) {
		var hashedPassword = textHasher.MCR.getReturnValue("hashText", 0);
		var hashedPasswordAtomic = dataFactory.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", SECRET, hashedPassword);
		systemSecretGroup.MCR.assertCalledParameters("addChild", hashedPasswordAtomic);
	}

	@Test
	public void testUpdateSecretInSystemSecret() throws Exception {
		DataRecordGroupSpy existingSystemSecret = new DataRecordGroupSpy();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> existingSystemSecret);

		systemSecretOperations.updateSecretForASystemSecret("someSystemSecretId", "someDataDivider",
				"someSecret");

		recordStorage.MCR.assertParameters("read", 0, SYSTEM_SECRET_TYPE, "someSystemSecretId");
		existingSystemSecret.MCR.assertParameters("removeAllChildrenWithNameInData", 0, SECRET);

		assertHashedSecretIsAddedToGroupAsNumber(existingSystemSecret);

		DataGroup existingSystemSecretTopDataGroup = (DataGroup) dataFactory.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup",
						existingSystemSecret);

		recordStorage.MCR.assertParameters("update", 0, SYSTEM_SECRET_TYPE, "someSystemSecretId",
				existingSystemSecretTopDataGroup, Collections.emptySet(), Collections.emptySet(),
				"someDataDivider");

	}

	private void assertHashedSecretIsAddedToGroupAsNumber(DataRecordGroupSpy systemSecretGroup) {
		var hashedPassword = textHasher.MCR.getReturnValue("hashText", 0);
		var hashedPasswordAtomic = dataFactory.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", SECRET, hashedPassword);
		systemSecretGroup.MCR.assertCalledParameters("addChild", hashedPasswordAtomic);
	}
}
