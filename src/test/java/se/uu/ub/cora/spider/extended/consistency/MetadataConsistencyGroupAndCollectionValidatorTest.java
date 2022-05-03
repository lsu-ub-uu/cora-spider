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

import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class MetadataConsistencyGroupAndCollectionValidatorTest {
	private RecordStorage recordStorage;
	private ExtendedFunctionality validator;
	private String recordType;
	private DataGroup recordAsDataGroup;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private String authToken = "someAuthToken";

	@BeforeMethod
	public void setUpDefaults() {
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		recordStorage = new RecordStorageCreateUpdateSpy();
		recordType = "metadataGroup";
		recordAsDataGroup = new DataGroupOldSpy("nameInData");
	}

	private void setUpDependencies() {
		validator = new MetadataConsistencyGroupAndCollectionValidator(recordStorage, recordType);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: childItem: childTwo does not exist in parent")
	public void testMetadataGroupChildDoesNotExistInParent() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		recordAsDataGroup = DataCreator2.createMetadataGroupWithTwoChildren();
		DataGroup refParentId = new DataGroupOldSpy("refParentId");
		refParentId.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		refParentId.addChild(new DataAtomicSpy("linkedRecordId", "testGroup"));
		recordAsDataGroup.addChild(refParentId);
		setUpDependencies();
		callValidatorUseExtendedFunctionality();
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
	public void testCollectionVariableFinalValueExistInCollection() {
		recordType = "metadataCollectionVariable";
		recordAsDataGroup = DataCreator2.createMetadataGroupWithCollectionVariableAsChild();

		recordAsDataGroup.addChild(new DataAtomicSpy("finalValue", "that"));
		setUpDependencies();
		exceptNoException();
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
