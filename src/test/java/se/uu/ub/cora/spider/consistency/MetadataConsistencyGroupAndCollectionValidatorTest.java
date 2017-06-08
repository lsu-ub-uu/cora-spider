/*
 * Copyright 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.consistency;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class MetadataConsistencyGroupAndCollectionValidatorTest {
	private RecordStorage recordStorage;
	private MetadataConsistencyValidator validator;
	private String recordType;
	private SpiderDataGroup recordAsSpiderDataGroup;

	@BeforeMethod
	public void setUpDefaults() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		recordType = "metadataGroup";
		recordAsSpiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
	}

	private void setUpDependencies() {
		validator = new MetadataConsistencyGroupAndCollectionValidatorImp(recordStorage,
				recordType);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: childItem: childTwo does not exist in parent")
	public void testMetadataGroupChildDoesNotExistInParent() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithTwoChildren();
		SpiderDataGroup refParentId = SpiderDataGroup.withNameInData("refParentId");
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "metadataGroup"));
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "testGroup"));
		recordAsSpiderDataGroup
				.addChild(refParentId);
		setUpDependencies();
		validator.validateRules(recordAsSpiderDataGroup);
	}

	@Test
	public void testMetadataGroupChildWithDifferentIdButSameNameInDataExistInParent() {

		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithTwoChildren();
		SpiderDataGroup refParentId = SpiderDataGroup.withNameInData("refParentId");
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "metadataGroup"));
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "testGroupWithTwoChildren"));
		recordAsSpiderDataGroup.addChild(refParentId);
		setUpDependencies();
		exceptNoException();
	}

	private void exceptNoException() {
		try {
			validator.validateRules(recordAsSpiderDataGroup);
		} catch (Exception e) {
			assertTrue(false);
		}
	}

	@Test
	public void testMetadataGroupChildWithOneChild() {
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithOneChild();
		SpiderDataGroup refParentId = SpiderDataGroup.withNameInData("refParentId");
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "metadataGroup"));
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "testGroupWithOneChild"));

		recordAsSpiderDataGroup.addChild(refParentId);
		setUpDependencies();
		exceptNoException();
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: referenced child:  does not exist")
	public void testMetadataGroupChildDoesNotExistInStorage() {
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithThreeChildren();

		SpiderDataGroup refParentId = SpiderDataGroup.withNameInData("refParentId");
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "metadataGroup"));
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "testGroupWithThreeChildren"));

		recordAsSpiderDataGroup.addChild(refParentId);
		setUpDependencies();
		validator.validateRules(recordAsSpiderDataGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: childItem: thatItem does not exist in parent")
	public void testCollectionVariableItemDoesNotExistInParent() {
		recordType = "metadataCollectionVariable";
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithCollectionVariableAsChild();

		SpiderDataGroup refParentId = SpiderDataGroup.withNameInData("refParentId");
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "metadataGroup"));
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "testParentMissingItemCollectionVar"));

		recordAsSpiderDataGroup.addChild(refParentId);
		setUpDependencies();
		validator.validateRules(recordAsSpiderDataGroup);
	}

	@Test
	public void testCollectionVariableItemExistInParent() {
		recordType = "metadataCollectionVariable";
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithCollectionVariableAsChild();

		SpiderDataGroup refParentId = SpiderDataGroup.withNameInData("refParentId");
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "metadataGroup"));
		refParentId.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "testParentCollectionVar"));

		recordAsSpiderDataGroup.addChild(refParentId);
		setUpDependencies();
		exceptNoException();
	}

	@Test
	public void testCollectionVariableFinalValueExistInCollection() {
		recordType = "metadataCollectionVariable";
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithCollectionVariableAsChild();

		recordAsSpiderDataGroup
				.addChild(SpiderDataAtomic.withNameInDataAndValue("finalValue", "that"));
		setUpDependencies();
		exceptNoException();
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: final value does not exist in collection")
	public void testCollectionVariableFinalValueDoesNotExistInCollection() {
		recordType = "metadataCollectionVariable";
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithCollectionVariableAsChild();

		recordAsSpiderDataGroup
				.addChild(SpiderDataAtomic.withNameInDataAndValue("finalValue", "doesNotExist"));
		setUpDependencies();
		validator.validateRules(recordAsSpiderDataGroup);
	}

	@Test
	public void testMetadataTypeThatHasNoInheritanceRules() {
		recordType = "metadataRecordLink";
		recordAsSpiderDataGroup = DataCreator.createMetadataGroupWithRecordLinkAsChild();

		recordAsSpiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("refParentId", "testParentRecordLink"));
		setUpDependencies();
		exceptNoException();
	}
}
