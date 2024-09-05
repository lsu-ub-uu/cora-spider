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
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class MetadataConsistencyGroupAndCollectionValidatorTest {
	private static final String METADATA = "metadata";
	private RecordStorageCreateUpdateSpy recordStorage;
	private ExtendedFunctionality validator;
	private DataGroup recordAsDataGroup;
	private String authToken = "someAuthToken";
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataFactorySpy dataFactorySpy;

	@BeforeMethod
	public void setUpDefaults() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		recordStorage = new RecordStorageCreateUpdateSpy();
		recordAsDataGroup = new DataGroupOldSpy("nameInData");
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				(Supplier<RecordStorage>) () -> recordStorage);

	}

	private void setUpDependencies() {
		validator = new MetadataConsistencyGroupAndCollectionValidator(dependencyProvider,
				METADATA);
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
		recordAsDataGroup.addChild(
				DataCreator2.createLinkWithLinkedId("refParentId", METADATA, "testGroup"));
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testMetadataGroupChildDoesNotExistInParentCatch() {

		recordAsDataGroup = DataCreator2.createMetadataGroupWithTwoChildren();
		recordAsDataGroup.addChild(
				DataCreator2.createLinkWithLinkedId("refParentId", METADATA, "testGroup"));
		setUpDependencies();
		try {
			callValidatorUseExtendedFunctionality();
			fail("Should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof DataException);

			recordStorage.MCR.assertNumberOfCallsToMethod("read", 6);
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 0, "id", "childOne");
			recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(METADATA));
			// metadataGroup
			recordStorage.MCR.assertParameter("read", 1, "id", "testGroup");
			recordStorage.MCR.assertParameterAsEqual("read", 1, "types", List.of(METADATA));
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 2, "id", "childOne");
			recordStorage.MCR.assertParameterAsEqual("read", 2, "types", List.of(METADATA));
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 3, "id", "childTwo");
			recordStorage.MCR.assertParameterAsEqual("read", 3, "types", List.of(METADATA));
			// metadataGroup
			recordStorage.MCR.assertParameter("read", 4, "id", "testGroup");
			recordStorage.MCR.assertParameterAsEqual("read", 4, "types", List.of(METADATA));
			// metadataGroup X
			recordStorage.MCR.assertParameter("read", 5, "id", "childOne");
			recordStorage.MCR.assertParameterAsEqual("read", 5, "types", List.of(METADATA));

			dependencyProvider.MCR.assertMethodNotCalled("getRecordTypeHandler");
			recordStorage.MCR.assertParameter("read", 0, "id", "childOne");
			recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(METADATA));

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
		recordAsDataGroup.addChild(DataCreator2.createLinkWithLinkedId("refParentId", METADATA,
				"testGroupWithTwoChildren"));
		setUpDependencies();
		exceptNoException();
	}

	private void exceptNoException() {
		try {
			callValidatorUseExtendedFunctionality();
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void testMetadataGroupChildWithOneChild() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithOneChild();
		recordAsDataGroup.addChild(DataCreator2.createLinkWithLinkedId("refParentId", METADATA,
				"testGroupWithOneChild"));
		setUpDependencies();
		exceptNoException();
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: referenced child:  does not exist")
	public void testMetadataGroupChildDoesNotExistInStorage() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithThreeChildren();
		recordAsDataGroup.addChild(DataCreator2.createLinkWithLinkedId("refParentId", METADATA,
				"testGroupWithThreeChildren"));
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testMetadataGroupWithoutTypeAttributeIsNotValidated() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithThreeChildren();
		recordAsDataGroup.addChild(DataCreator2.createLinkWithLinkedId("refParentId", METADATA,
				"testGroupWithThreeChildren"));
		((DataGroupOldSpy) recordAsDataGroup).MRV.setSpecificReturnValuesSupplier(
				"getAttributeValue", () -> Optional.empty(), "type");

		setUpDependencies();

		callValidatorUseExtendedFunctionality();

		exceptNoException();
	}

	@Test
	public void testMetadataGroupChildDoesNotExistInStorageExceptionIsSentAlong() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithThreeChildren();
		recordAsDataGroup.addChild(DataCreator2.createLinkWithLinkedId("refParentId", METADATA,
				"testGroupWithThreeChildren"));
		setUpDependencies();
		try {
			callValidatorUseExtendedFunctionality();
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof RecordNotFoundException);
		}
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: childItem: thatItem does not exist in parent")
	public void testCollectionVariableItemDoesNotExistInParent() {
		recordAsDataGroup = DataCreator2.createDataGroupDescribingACollectionVariable();
		recordAsDataGroup.addChild(DataCreator2.createLinkWithLinkedId("refParentId", METADATA,
				"testParentMissingItemCollectionVar"));
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testCollectionVariableItemExistInParent() {
		recordAsDataGroup = DataCreator2.createDataGroupDescribingACollectionVariable();
		recordAsDataGroup.addChild(DataCreator2.createLinkWithLinkedId("refParentId", METADATA,
				"testParentCollectionVar"));
		setUpDependencies();
		exceptNoException();

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 3);
		recordStorage.MCR.assertParameter("read", 0, "id", "testItemCollection");
		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(METADATA));

		recordStorage.MCR.assertParameter("read", 1, "id", "testParentCollectionVar");
		recordStorage.MCR.assertParameterAsEqual("read", 1, "types", List.of(METADATA));

		recordStorage.MCR.assertParameter("read", 2, "id", "testParentItemCollection");
		recordStorage.MCR.assertParameterAsEqual("read", 2, "types", List.of(METADATA));
	}

	@Test
	public void testCollectionVariableFinalValueExistInCollection() throws Exception {
		recordAsDataGroup = DataCreator2.createDataGroupDescribingACollectionVariable();
		recordAsDataGroup.addChild(new DataAtomicOldSpy("finalValue", "that"));
		setUpDependencies();
		callValidatorUseExtendedFunctionality();

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 3);

		recordStorage.MCR.assertParameter("read", 0, "id", "testItemCollection");
		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(METADATA));

		recordStorage.MCR.assertParameter("read", 1, "id", "thisItem");
		recordStorage.MCR.assertParameterAsEqual("read", 1, "types", List.of(METADATA));

		recordStorage.MCR.assertParameter("read", 2, "id", "thatItem");
		recordStorage.MCR.assertParameterAsEqual("read", 2, "types", List.of(METADATA));
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: final value does not exist in collection")
	public void testCollectionVariableFinalValueDoesNotExistInCollection() {
		recordAsDataGroup = DataCreator2.createDataGroupDescribingACollectionVariable();
		recordAsDataGroup.addChild(new DataAtomicOldSpy("finalValue", "doesNotExist"));
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
	}

	@Test
	public void testMetadataTypeThatHasNoInheritanceRules() {
		recordAsDataGroup = DataCreator2.createMetadataGroupWithRecordLinkAsChild();
		recordAsDataGroup.addChild(new DataAtomicOldSpy("refParentId", "testParentRecordLink"));
		setUpDependencies();
		exceptNoException();
	}
}
