/*
 * Copyright 2016, 2022 Uppsala University Library
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

package se.uu.ub.cora.spider.extended.consistency;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class MetadataConsistencyGroupAndCollectionValidatorTest {
	private RecordStorageCreateUpdateSpy recordStorage;
	private ExtendedFunctionality validator;
	private String recordType;
	private DataGroup recordAsDataGroup;
	private String authToken = "someAuthToken";
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataFactorySpy dataFactorySpy;

	@BeforeMethod
	public void setUpDefaults() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		recordStorage = new RecordStorageCreateUpdateSpy();
		recordType = "metadataGroup";
		recordAsDataGroup = new DataGroupOldSpy("nameInData");
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				(Supplier<RecordStorage>) () -> recordStorage);

	}

	private void setUpDependencies() {
		validator = new MetadataConsistencyGroupAndCollectionValidator(dependencyProvider,
				recordType);
	}

	@Test
	public void testOnlyForTest() throws Exception {
		setUpDependencies();
		assertSame(dependencyProvider, ((MetadataConsistencyGroupAndCollectionValidator) validator)
				.onlyForTestGetDependencyProvider());
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: childItem: childTwo does not exist in parent")
	public void testMetadataGroupChildDoesNotExistInParent() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithTwoChildren();
		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testGroup"));
		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testMetadataGroupChildDoesNotExistInParentCatch() {

		recordAsDataGroup = DataCreator2.createMetadataGroupWithTwoChildren();
		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testGroup"));
		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		try {
			callValidatorUseExtendedFunctionality();
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof DataException);

			recordStorage.MCR.assertNumberOfCallsToMethod("read", 6);
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 0, "id", "childOne");
			// metadataGroup
			recordStorage.MCR.assertParameter("read", 1, "id", "testGroup");
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 2, "id", "childOne");
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 3, "id", "childTwo");
			// metadataGroup
			recordStorage.MCR.assertParameter("read", 4, "id", "testGroup");
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 5, "id", "childOne");

			dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 4);
			dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "metadataGroup");
			dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "metadataGroup");
			dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "metadataGroup");
			dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 3, "metadataGroup");

			RecordTypeHandlerSpy recordTypeHandler = (RecordTypeHandlerSpy) dependencyProvider.MCR
					.getReturnValue("getRecordTypeHandler", 0);

			var types = recordTypeHandler.MCR
					.getReturnValue("getListOfRecordTypeIdsToReadFromStorage", 0);

			recordStorage.MCR.assertParameters("read", 0, types, "childOne");

		}
	}

	private void callValidatorUseExtendedFunctionality() {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = authToken;
		data.dataGroup = recordAsDataGroup;
		validator.useExtendedFunctionality(data);
	}

	@Test
	public void testMetadataGroupChildWithDifferentIdButSameNameInDataExistInParent() {

		recordAsDataGroup = DataCreator2.createMetadataGroupWithTwoChildren();
		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testGroupWithTwoChildren"));
		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		exceptNoException();
	}

	private void exceptNoException() {
		try {
			callValidatorUseExtendedFunctionality();
		} catch (Exception e) {
			assertTrue(false);
		}
	}

	@Test
	public void testMetadataGroupChildWithOneChild() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithOneChild();
		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testGroupWithOneChild"));

		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		exceptNoException();
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: referenced child:  does not exist")
	public void testMetadataGroupChildDoesNotExistInStorage() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithThreeChildren();

		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testGroupWithThreeChildren"));

		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testMetadataGroupChildDoesNotExistInStorageExceptionIsSentAlong() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithThreeChildren();

		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testGroupWithThreeChildren"));

		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		try {
			callValidatorUseExtendedFunctionality();
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof RecordNotFoundException);
		}
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: childItem: thatItem does not exist in parent")
	public void testCollectionVariableItemDoesNotExistInParent() {
		recordType = "metadataCollectionVariable";
		recordAsDataGroup = DataCreator2.createMetadataGroupWithCollectionVariableAsChild();

		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(
				new DataAtomicSpy("linkedRecordId", "testParentMissingItemCollectionVar"));

		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testCollectionVariableItemExistInParent() {
		recordType = "metadataCollectionVariable";
		recordAsDataGroup = DataCreator2.createMetadataGroupWithCollectionVariableAsChild();

		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testParentCollectionVar"));

		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		exceptNoException();
	}

	@Test
	public void testCollectionVariableFinalValueExistInCollection() throws Exception {
		recordType = "metadataCollectionVariable";
		recordAsDataGroup = DataCreator2.createMetadataGroupWithCollectionVariableAsChild();

		recordAsDataGroup.addChild(new DataAtomicSpy("finalValue", "that"));
		setUpDependencies();

		callValidatorUseExtendedFunctionality();

		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0,
				"metadataCollectionItem");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1,
				"metadataCollectionItem");

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 3);

		RecordTypeHandlerSpy recordTypeHandler = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 0);
		var types1 = recordTypeHandler.MCR.getReturnValue("getListOfRecordTypeIdsToReadFromStorage",
				0);
		recordStorage.MCR.assertParameters("read", 1, types1, "thisItem");

		RecordTypeHandlerSpy recordTypeHandler2 = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 1);
		var types2 = recordTypeHandler2.MCR
				.getReturnValue("getListOfRecordTypeIdsToReadFromStorage", 0);
		recordStorage.MCR.assertParameters("read", 2, types2, "thatItem");
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: final value does not exist in collection")
	public void testCollectionVariableFinalValueDoesNotExistInCollection() {
		recordType = "metadataCollectionVariable";
		recordAsDataGroup = DataCreator2.createMetadataGroupWithCollectionVariableAsChild();

		recordAsDataGroup.addChild(new DataAtomicSpy("finalValue", "doesNotExist"));
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testMetadataTypeThatHasNoInheritanceRules() {
		recordType = "metadataRecordLink";
		recordAsDataGroup = DataCreator2.createMetadataGroupWithRecordLinkAsChild();

		recordAsDataGroup.addChild(new DataAtomicSpy("refParentId", "testParentRecordLink"));
		setUpDependencies();
		exceptNoException();
	}
}
