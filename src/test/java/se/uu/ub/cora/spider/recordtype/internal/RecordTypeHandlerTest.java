/*
 * Copyright 2016, 2019, 2020, 2021, 2022 Uppsala University Library
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

package se.uu.ub.cora.spider.recordtype.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.record.RecordStorageLightSpy;
import se.uu.ub.cora.spider.record.RecordStorageMCRSpy;
import se.uu.ub.cora.spider.record.internal.DataGroupCheckCallsSpy;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;

public class RecordTypeHandlerTest {
	private DataFactorySpy dataFactorySpy;
	private static final String ABSTRACT = "abstract";
	private RecordStorageLightSpy recordStorageLightSpy;
	private String defaultRecordTypeId = "someRecordType";
	private RecordTypeHandler recordTypeHandler;
	private RecordStorageMCRSpy recordStorage;
	private RecordTypeHandlerFactorySpy recordTypeHandlerFactory;

	@BeforeMethod
	public void setUp() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		recordStorage = new RecordStorageMCRSpy();
		recordTypeHandlerFactory = new RecordTypeHandlerFactorySpy();
		recordStorageLightSpy = new RecordStorageLightSpy();
	}

	@Test
	public void testInitializeFromStorage() throws Exception {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorage, "someId");

		recordStorage.MCR.assertParameters("read", 0, "recordType", "someId");
		assertEquals(recordTypeHandler.getRecordTypeId(), "someId");
	}

	@Test
	public void testInitializeFromDataGroup() throws Exception {
		DataGroupSpy dataGroup = createTopDataGroup();

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndDataGroup(null, recordStorage, dataGroup);

		recordStorage.MCR.assertMethodNotCalled("read");
		assertRecordTypeIdFetchedFromEnteredData(dataGroup, recordTypeHandler);
	}

	private DataGroupSpy createTopDataGroup() {
		DataGroupSpy dataGroup = new DataGroupSpy();
		DataGroupSpy recordInfoGroup = new DataGroupSpy();
		recordInfoGroup.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				(Supplier<String>) () -> "recordIdFromSpy");
		dataGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				(Supplier<DataGroupSpy>) () -> recordInfoGroup, "recordInfo");
		return dataGroup;
	}

	private void assertRecordTypeIdFetchedFromEnteredData(DataGroupSpy dataGroup,
			RecordTypeHandler recordTypeHandler) {
		DataGroupSpy recordInfoGroup = (DataGroupSpy) dataGroup.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);
		String idFromSpy = (String) recordInfoGroup.MCR
				.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		assertEquals(recordTypeHandler.getRecordTypeId(), idFromSpy);
	}

	@Test
	public void testAbstract() {
		setupForStorageAtomicValue(ABSTRACT, "true");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroupMCR = getRecordTypeDataGroupReadFromStorage();
		assertAbstract(dataGroupMCR, true);
	}

	private void assertAbstract(DataGroupSpy dataGroupMCR, boolean expected) {
		assertEquals(recordTypeHandler.isAbstract(), expected);
		dataGroupMCR.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, ABSTRACT);
	}

	private DataGroupSpy getRecordTypeDataGroupReadFromStorage() {
		DataGroupSpy dataGroup = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
		return dataGroup;
	}

	private void setupForStorageAtomicValue(String nameInData, String value) {
		DataGroupSpy DataGroupSpy = new DataGroupSpy();
		DataGroupSpy.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(value),
				nameInData);
		recordStorage.dataGroupForRead = DataGroupSpy;
	}

	private DataGroupSpy setupDataGroupWithAtomicValue(String nameInData, String value) {
		DataGroupSpy topDataGroup = createTopDataGroup();
		topDataGroup.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(value),
				nameInData);
		return topDataGroup;
	}

	private DataGroupSpy setupForAtomicValue(String nameInData, String value) {
		DataGroupSpy dataGroup = new DataGroupSpy();
		dataGroup.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(value),
				nameInData);
		return dataGroup;
	}

	@Test
	public void testAbstractFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue(ABSTRACT, "true");

		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertAbstract(dataGroup, true);
	}

	@Test
	public void testAbstractIsFalse() {
		setupForStorageAtomicValue(ABSTRACT, "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertAbstract(dataGroup, false);
	}

	@Test
	public void testAbstractIsFalseFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue(ABSTRACT, "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertAbstract(dataGroup, false);
	}

	@Test
	public void testShouldAutoGenerateId() {
		setupForStorageAtomicValue("userSuppliedId", "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertShouldAutoGenerateId(dataGroup, true);
	}

	@Test
	public void testShouldAutoGenerateIdFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue("userSuppliedId", "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertShouldAutoGenerateId(dataGroup, true);
	}

	private void assertShouldAutoGenerateId(DataGroupSpy dataGroupMCR, boolean expected) {
		assertEquals(recordTypeHandler.shouldAutoGenerateId(), expected);
		dataGroupMCR.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "userSuppliedId");
	}

	@Test
	public void testShouldAutoGenerateIdFalse() {
		setupForStorageAtomicValue("userSuppliedId", "true");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertShouldAutoGenerateId(dataGroup, false);
	}

	@Test
	public void testShouldAutoGenerateIdFalseFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue("userSuppliedId", "true");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertShouldAutoGenerateId(dataGroup, false);
	}

	@Test
	public void testGetNewMetadataId() {
		setupForLinkForStorageWithNameInDataAndRecordId("newMetadataId", "someNewMetadataId");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someRecordId");

		String newMetadataId = recordTypeHandler.getNewMetadataId();

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertUsedLink(dataGroup, "newMetadataId", newMetadataId);
	}

	@Test
	public void testGetNewMetadataIdFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithLinkUsingNameInDataAndRecordId("newMetadataId",
				"someNewMetadataId");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		String newMetadataId = recordTypeHandler.getNewMetadataId();

		assertUsedLinkFromDataGroup(dataGroup, "newMetadataId", newMetadataId);
	}

	@Test
	public void testGetMetadataId() {
		setupForLinkForStorageWithNameInDataAndRecordId("metadataId", "someMetadataId");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someRecordId");

		String metadataId = recordTypeHandler.getMetadataId();

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertUsedLink(dataGroup, "metadataId", metadataId);
	}

	@Test
	public void testGetMetadataIdFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithLinkUsingNameInDataAndRecordId("metadataId",
				"someMetadataId");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		String metadataId = recordTypeHandler.getMetadataId();

		assertUsedLinkFromDataGroup(dataGroup, "metadataId", metadataId);
	}

	private void assertUsedLink(DataGroupSpy topGroup, String linkNameInData,
			String returnedLinkedRecordId) {
		int callNumber = 0;
		assertUsedLinkCallNo(topGroup, linkNameInData, returnedLinkedRecordId, callNumber);
	}

	private void assertUsedLinkFromDataGroup(DataGroupSpy topGroup, String linkNameInData,
			String returnedLinkedRecordId) {
		int callNumber = 1;
		assertUsedLinkCallNo(topGroup, linkNameInData, returnedLinkedRecordId, callNumber);
	}

	private void assertUsedLinkCallNo(DataGroupSpy topGroup, String linkNameInData,
			String returnedLinkedRecordId, int callNumber) {
		topGroup.MCR.assertParameters("getFirstGroupWithNameInData", callNumber, linkNameInData);

		DataGroupSpy linkGroup = (DataGroupSpy) topGroup.MCR
				.getReturnValue("getFirstGroupWithNameInData", callNumber);
		String linkedRecordIdFromSpy = (String) linkGroup.MCR
				.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		assertEquals(returnedLinkedRecordId, linkedRecordIdFromSpy);
	}

	private void setupForLinkForStorageWithNameInDataAndRecordId(String linkNameInData,
			String linkedRecordId) {
		DataGroupSpy dataGroup = setupForLinkWithNameInDataAndRecordId(linkNameInData,
				linkedRecordId);
		recordStorage.dataGroupForRead = dataGroup;
	}

	private DataGroupSpy setupForLinkWithNameInDataAndRecordId(String linkNameInData,
			String linkedRecordId) {
		DataGroupSpy dataGroup = new DataGroupSpy();
		DataGroupSpy link = setupForAtomicValue("linkedRecordId", linkedRecordId);
		dataGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(true), linkNameInData);
		dataGroup.MRV.setReturnValues("getFirstGroupWithNameInData", List.of(link), linkNameInData);
		return dataGroup;
	}

	private DataGroupSpy setupDataGroupWithLinkUsingNameInDataAndRecordId(String linkNameInData,
			String linkedRecordId) {
		DataGroupSpy topDataGroup = createTopDataGroup();
		DataGroupSpy link = setupForAtomicValue("linkedRecordId", linkedRecordId);
		topDataGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(true),
				linkNameInData);
		topDataGroup.MRV.setReturnValues("getFirstGroupWithNameInData", List.of(link),
				linkNameInData);
		return topDataGroup;
	}

	@Test
	public void testPublic() {
		setupForStorageAtomicValue("public", "true");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertIsPublicForRead(dataGroup, true);
	}

	private void assertIsPublicForRead(DataGroupSpy dataGroupMCR, boolean expected) {
		assertEquals(recordTypeHandler.isPublicForRead(), expected);
		dataGroupMCR.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "public");
	}

	@Test
	public void testPublicFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue("public", "true");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertIsPublicForRead(dataGroup, true);
	}

	@Test
	public void testPublicFalse() {
		setupForStorageAtomicValue("public", "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertIsPublicForRead(dataGroup, false);
	}

	@Test
	public void testPublicFalseFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue("public", "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertIsPublicForRead(dataGroup, false);
	}

	@Test
	public void testGetMetadataGroup() {
		RecordTypeHandler recordTypeHandler = setUpRecordTypeWithMetadataIdForStorage();
		DataGroup metadataGroup = recordTypeHandler.getMetadataGroup();

		recordStorage.MCR.assertParameters("read", 0, "recordType", "someRecordTypeId");
		recordStorage.MCR.assertParameters("read", 1, "metadataGroup", "someMetadataId");

		DataGroupSpy returnValue = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 1);
		assertSame(metadataGroup, returnValue);
	}

	private RecordTypeHandler setUpRecordTypeWithMetadataIdForStorage() {
		setupForLinkForStorageWithNameInDataAndRecordId("metadataId", "someMetadataId");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorage, "someRecordTypeId");
		return recordTypeHandler;
	}

	@Test
	public void testGetMetadataGroupTwiceReturnsSameInstance() {
		RecordTypeHandler recordTypeHandler = setUpRecordTypeWithMetadataIdForStorage();

		DataGroup metadataGroup = recordTypeHandler.getMetadataGroup();
		assertEquals(recordStorage.MCR.getNumberOfCallsToMethod("read"), 2);

		DataGroup metadataGroup2 = recordTypeHandler.getMetadataGroup();
		assertEquals(recordStorage.MCR.getNumberOfCallsToMethod("read"), 2);
		assertSame(metadataGroup, metadataGroup2);
	}

	@Test
	public void testGetMetadataGroupFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithLinkUsingNameInDataAndRecordId("metadataId",
				"someMetadataId");

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndDataGroup(null, recordStorage, dataGroup);

		DataGroup metadataGroup = recordTypeHandler.getMetadataGroup();

		recordStorage.MCR.assertParameters("read", 0, "metadataGroup", "someMetadataId");
		DataGroupSpy returnValue = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
		assertSame(metadataGroup, returnValue);

		DataGroup metadataGroup2 = recordTypeHandler.getMetadataGroup();
		assertSame(metadataGroup, metadataGroup2);
	}

	@Test
	public void testGetCombinedIdsUsingRecordIdNoParent() {
		DataGroupSpy dataGroup = new DataGroupSpy();
		dataGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(false), "parentId");
		recordStorage.dataGroupForRead = dataGroup;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorage, "someRecordType");

		List<String> ids = recordTypeHandler.getCombinedIdsUsingRecordId("someRecordTypeId");

		assertEquals(ids.size(), 1);
		assertEquals(ids.get(0), "someRecordType_someRecordTypeId");
	}

	@Test
	public void testGetCombinedIdsUsingRecordIdWithParent() {
		setupForLinkForStorageWithNameInDataAndRecordId("parentId", "parentRecordType");

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorage, "someRecordType");
		List<String> ids = recordTypeHandler.getCombinedIdsUsingRecordId("someRecordTypeId");
		assertEquals(ids.size(), 2);
		assertEquals(ids.get(0), "someRecordType_someRecordTypeId");
		assertEquals(ids.get(1), "parentRecordType_someRecordTypeId");
	}

	@Test
	public void testGetCombinedIdsUsingRecordIdFromDataGroupNoParent() {
		DataGroupSpy dataGroup = createTopDataGroup();
		// DataGroupSpy dataGroup = new DataGroupSpy();
		dataGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(false), "parentId");
		recordStorage.dataGroupForRead = dataGroup;

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndDataGroup(null, recordStorage, dataGroup);
		List<String> ids = recordTypeHandler.getCombinedIdsUsingRecordId("someRecordTypeId");
		assertEquals(ids.size(), 1);
		assertEquals(ids.get(0), "recordIdFromSpy_someRecordTypeId");
	}

	@Test
	public void testGetRecordPartReadConstraintsNOReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(storageSpy.type, "metadataGroup");
		assertEquals(storageSpy.ids.get(0), "organisation");
		assertTrue(recordPartReadConstraints.isEmpty());
		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertTrue(recordPartWriteConstraints.isEmpty());

		Set<Constraint> recordPartCreateWriteConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertTrue(recordPartCreateWriteConstraints.isEmpty());
	}

	private RecordTypeHandlerStorageSpy setUpHandlerWithStorageSpyUsingTypeId(String recordTypeId) {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null, storageSpy,
				recordTypeId);
		return storageSpy;
	}

	@Test
	public void testGetRecordPartReadConstraintsOneReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 1);

		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints, "organisationRoot",
				0);

		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 1);

		assertConstraintExistWithNumberOfAttributes(recordPartWriteConstraints, "organisationRoot",
				0);

		Set<Constraint> recordPartWriteCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartWriteCreateConstraints.size(), 1);

		assertConstraintExistWithNumberOfAttributes(recordPartWriteCreateConstraints,
				"organisationRoot2", 0);
	}

	private void assertConstraintExistWithNumberOfAttributes(
			Set<Constraint> recordPartReadConstraints, String constraintName, int numOfAttributes) {
		Constraint organisationReadConstraint = getConstraintByNameInData(recordPartReadConstraints,
				constraintName);
		assertEquals(organisationReadConstraint.getDataAttributes().size(), numOfAttributes);
	}

	private Constraint getConstraintByNameInData(Set<Constraint> constraints, String nameInData) {
		for (Constraint constraint : constraints) {
			if (constraint.getNameInData().equals(nameInData)) {
				return constraint;
			}
		}
		return null;
	}

	@Test
	public void testGetRecordPartReadConstraintsOneReadConstraintWithAttribute() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfAttributes = 1;

		assertCorrectReadConstraintsWithOneAttributeForOneChild();
		assertCorrectWriteConstraintsWithOneAttributeForOneChild();
		assertCorrectCreateWriteConstraintsWithOneAttributeForOneChild();

		dataFactorySpy.MCR.assertParameters("factorAttributeUsingNameInDataAndValue", 0, "type",
				"default");

	}

	private void assertCorrectReadConstraintsWithOneAttributeForOneChild() {
		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertCorrectConstraintsWithOneAttributeForOneChild(recordPartReadConstraints);
		assertCorrectConstraintsWithOneAttributeForOneChild(recordPartReadConstraints);
	}

	private void assertCorrectConstraintsWithOneAttributeForOneChild(
			Set<Constraint> recordPartReadConstraints) {
		assertEquals(recordPartReadConstraints.size(), 2);

		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints, "organisationRoot",
				0);
		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints,
				"organisationAlternativeName", 1);
	}

	private void assertCorrectWriteConstraintsWithOneAttributeForOneChild() {
		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertCorrectConstraintsWithOneAttributeForOneChild(recordPartWriteConstraints);
	}

	private void assertCorrectCreateWriteConstraintsWithOneAttributeForOneChild() {
		Set<Constraint> recordPartCreateWriteConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartCreateWriteConstraints.size(), 2);

		assertConstraintExistWithNumberOfAttributes(recordPartCreateWriteConstraints,
				"organisationRoot2", 0);
		assertConstraintExistWithNumberOfAttributes(recordPartCreateWriteConstraints,
				"organisationAlternativeName", 1);
	}

	@Test
	public void testGetRecordPartReadConstraintsOneReadConstraintWithTwoAttributes() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfAttributes = 2;

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 2);

		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints, "organisationRoot",
				0);
		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints,
				"organisationAlternativeName", 2);
	}

	// new stuff here, for children to max 1
	@Test
	public void testGetRecordPartReadConstraintsWithGrandChild() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 1;

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 3);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartReadConstraints);
	}

	private void assertCorrectConstraintsIncludingFirstLevelChild(
			Set<Constraint> recordPartReadConstraints) {
		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints, "organisationRoot",
				0);
		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints,
				"organisationAlternativeName", 0);
		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints, "showInPortal", 0);
	}

	@Test
	public void testGetRecordPartReadConstraintsWithGreatGrandChild() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 2;

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 4);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartReadConstraints);
		assertConstraintExistWithNumberOfAttributes(recordPartReadConstraints, "greatGrandChild",
				0);
	}

	@Test
	public void testGetRecordPartReadConstraintsWithGreatGrandChildNOTMax1() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 2;
		storageSpy.maxNoOfGrandChildren = "3";

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 3);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartReadConstraints);
	}

	/**********************************************************************/
	@Test
	public void testRecursiveChildOnlyTraversedOnce() {
		DataGroupOldSpy dataGroup = new DataGroupOldSpy("dataGroupNameInData");
		dataGroup.addChild(dataGroup);

		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationRecursiveChild");

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(storageSpy.types.size(), 4);
		assertEquals(storageSpy.ids.size(), 4);
		assertEquals(storageSpy.ids.get(0), "organisationRecursiveChild");
		assertEquals(storageSpy.ids.get(1), "organisationRecursiveChild");
		assertEquals(storageSpy.ids.get(2), "divaOrganisationRecursiveNameGroup");
		assertEquals(storageSpy.ids.get(3), "divaOrganisationRecursiveNameGroup");

		DataGroupOldSpy returnValueNameGroup = (DataGroupOldSpy) storageSpy.MCR
				.getReturnValue("read", 2);
		int recordInfoAndChildRefsRequested = 2;
		assertEquals(returnValueNameGroup.requestedDataGroups.size(),
				recordInfoAndChildRefsRequested);

		DataGroupOldSpy returnValueNameGroup2 = (DataGroupOldSpy) storageSpy.MCR
				.getReturnValue("read", 3);
		int onlyRecordInfoRequested = 1;
		assertEquals(returnValueNameGroup2.requestedDataGroups.size(), onlyRecordInfoRequested);
		assertTrue(recordPartReadConstraints.isEmpty());
	}

	/**********************************************************************/
	@Test
	public void testGetRecordPartReadWriteConstraintsWithGrandChild() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 1;

		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 3);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartWriteConstraints);
	}

	@Test
	public void testGetRecordPartReadWriteConstraintsWithGreatGrandChild() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 2;

		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 4);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartWriteConstraints);
		assertConstraintExistWithNumberOfAttributes(recordPartWriteConstraints, "greatGrandChild",
				0);
	}

	@Test
	public void testGetRecordPartReadWriteConstraintsWithGreatGrandChildNOTMax1() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 2;
		storageSpy.maxNoOfGrandChildren = "3";

		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 3);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartWriteConstraints);
	}

	@Test
	public void testGetRecordPartCreateWriteConstraintsWithGrandChild() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 1;
		storageSpy.useStandardMetadataGroupForNew = true;

		Set<Constraint> recordPartCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartCreateConstraints.size(), 3);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartCreateConstraints);
	}

	@Test
	public void testGetRecordPartCreateWriteConstraintsWithGreatGrandChild() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 2;
		storageSpy.useStandardMetadataGroupForNew = true;

		Set<Constraint> recordPartCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartCreateConstraints.size(), 4);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartCreateConstraints);
		assertConstraintExistWithNumberOfAttributes(recordPartCreateConstraints, "greatGrandChild",
				0);
	}

	@Test
	public void testGetRecordPartCreateWriteConstraintsWithGreatGrandChildNOTMax1() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisationChildWithAttribute");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfGrandChildrenWithReadWriteConstraint = 2;
		storageSpy.maxNoOfGrandChildren = "3";
		storageSpy.useStandardMetadataGroupForNew = true;

		Set<Constraint> recordPartCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartCreateConstraints.size(), 3);

		assertCorrectConstraintsIncludingFirstLevelChild(recordPartCreateConstraints);
	}

	@Test
	public void testGetRecordPartReadConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;

		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 4);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisation");
		storageSpy.MCR.assertParameters("read", 2, "metadataGroup", "divaOrganisationNameGroup");
		storageSpy.MCR.assertParameters("read", 3, "metadataTextVariable", "divaOrganisationRoot");
	}

	@Test
	public void testGetRecordPartWriteConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;

		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 4);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisation");
		storageSpy.MCR.assertParameters("read", 2, "metadataGroup", "divaOrganisationNameGroup");
		storageSpy.MCR.assertParameters("read", 3, "metadataTextVariable", "divaOrganisationRoot");
	}

	@Test
	public void testGetRecordPartCreateConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;

		recordTypeHandler.getRecordPartCreateWriteConstraints();
		recordTypeHandler.getRecordPartCreateWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 4);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisationNew");
		storageSpy.MCR.assertParameters("read", 2, "metadataGroup", "divaOrganisationNameGroup");
		storageSpy.MCR.assertParameters("read", 3, "metadataTextVariable", "divaOrganisationRoot2");
	}

	@Test
	public void testGetRecordPartReadConstraintsTwoReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");

		storageSpy.numberOfChildrenWithReadWriteConstraint = 2;
		Set<Constraint> recordPartReadForUpdateConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadForUpdateConstraints.size(), 2);

		assertTrue(containsConstraintWithNameInData(recordPartReadForUpdateConstraints,
				"organisationRoot"));
		assertTrue(containsConstraintWithNameInData(recordPartReadForUpdateConstraints,
				"showInPortal"));

		Set<Constraint> recordPartWriteForUpdateConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteForUpdateConstraints.size(), 2);

		assertTrue(containsConstraintWithNameInData(recordPartWriteForUpdateConstraints,
				"showInPortal"));
		assertTrue(containsConstraintWithNameInData(recordPartWriteForUpdateConstraints,
				"organisationRoot"));

		Set<Constraint> recordPartCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartCreateConstraints.size(), 2);
		assertTrue(
				containsConstraintWithNameInData(recordPartCreateConstraints, "organisationRoot2"));
		assertTrue(containsConstraintWithNameInData(recordPartCreateConstraints, "showInPortal2"));
	}

	@Test
	public void testGetRecordPartReadConstraintsOnlyReadWriteConstraintsAreAdded() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 2;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 2);

		assertTrue(containsConstraintWithNameInData(recordPartReadConstraints, "organisationRoot"));
		assertTrue(containsConstraintWithNameInData(recordPartReadConstraints, "showInPortal"));
		assertFalse(containsConstraintWithNameInData(recordPartReadConstraints, "showInDefence"));

		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 3);

		assertTrue(containsConstraintWithNameInData(recordPartWriteConstraints, "showInPortal"));
		assertTrue(containsConstraintWithNameInData(recordPartWriteConstraints, "showInDefence"));
		assertTrue(
				containsConstraintWithNameInData(recordPartWriteConstraints, "organisationRoot"));

		Set<Constraint> recordPartCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartCreateConstraints.size(), 3);
		assertTrue(
				containsConstraintWithNameInData(recordPartCreateConstraints, "organisationRoot2"));
		assertTrue(containsConstraintWithNameInData(recordPartCreateConstraints, "showInPortal2"));
		assertTrue(containsConstraintWithNameInData(recordPartCreateConstraints, "showInDefence2"));
	}

	private boolean containsConstraintWithNameInData(Set<Constraint> constraints,
			String nameInData) {

		for (Constraint constraint : constraints) {
			if (constraint.getNameInData().equals(nameInData)) {
				return true;
			}
		}

		return false;
	}

	@Test
	public void testHasRecordPartReadConstraintsNoConstraints() {
		setUpHandlerWithStorageSpyUsingTypeId("organisation");
		assertFalse(recordTypeHandler.hasRecordPartReadConstraint());
	}

	@Test
	public void testHasRecordPartReadConstraintsOneConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		assertTrue(recordTypeHandler.hasRecordPartReadConstraint());
	}

	@Test
	public void testHasRecordPartWriteConstraintsNoConstraints() {
		setUpHandlerWithStorageSpyUsingTypeId("organisation");
		assertFalse(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasRecordPartWriteConstraintsOneReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfChildrenWithWriteConstraint = 0;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasRecordPartWriteConstraintsOneWriteConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 0;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasRecordPartCreateConstraintsNoConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, storageSpy, "organisation");
		assertFalse(recordTypeHandler.hasRecordPartCreateConstraint());
	}

	@Test
	public void testHasRecordPartCreateConstraintsOneReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfChildrenWithWriteConstraint = 0;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartCreateConstraint());
	}

	@Test
	public void testHasRecordPartCreateConstraintsOneWriteConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 0;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartCreateConstraint());
	}

	@Test
	public void testHasParent() {
		addParentIdToContain(recordStorageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean hasParent = recordTypeHandler.hasParent();
		assertContainsWasRequestedForParentId();
		assertTrue(hasParent);
	}

	private void assertContainsWasRequestedForParentId() {
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;
		assertEquals((recordTypeDataGroup.nameInDatasSentToContains.get(0)), "parentId");
	}

	@Test
	public void testHasNoParent() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);
		assertFalse(recordTypeHandler.hasParent());
	}

	private void addParentIdToContain(RecordStorageLightSpy recordStorgageSpy, String id) {
		String childNameInData = "parentId";
		addChildToGroupIdReturnedFromStorageSpy(childNameInData, id, recordStorgageSpy);
	}

	private void addChildToGroupIdReturnedFromStorageSpy(String childNameInData, String id,
			RecordStorageLightSpy recordStorgageSpy) {
		List<String> childrenToContain = new ArrayList<>();
		childrenToContain.add(childNameInData);
		recordStorgageSpy.childrenToContainInDataGroup.put(id, childrenToContain);
	}

	@Test
	public void testGetParentId() {
		addParentIdToContain(recordStorageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		String parentId = recordTypeHandler.getParentId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;

		assertParentIdWasRequestedFromDataGroup(recordTypeDataGroup);
		assertLinkedRecordIdWasRequestedAndReturned(recordTypeDataGroup, parentId);
	}

	private void assertParentIdWasRequestedFromDataGroup(
			DataGroupCheckCallsSpy dataGroupReturnedFromSpy) {
		assertEquals((dataGroupReturnedFromSpy.requestedFirstGroupWithNameInData.get(0)),
				"parentId");
	}

	private void assertLinkedRecordIdWasRequestedAndReturned(
			DataGroupCheckCallsSpy dataGroupReturnedFromSpy, String parentId) {
		DataGroupCheckCallsSpy returnedParentGroup = dataGroupReturnedFromSpy.returnedChildrenGroups
				.get(0);
		assertEquals(returnedParentGroup.requestedFirstAtomicValue.get(0), "linkedRecordId");
		assertEquals(parentId, returnedParentGroup.returnedAtomicValues.get(0));
	}

	@Test(expectedExceptions = DataMissingException.class, expectedExceptionsMessageRegExp = ""
			+ "Unable to get parentId, no parents exists")
	public void testGetParentIdThrowsExceptionWhenMissing() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		recordTypeHandler.getParentId();
	}

	@Test
	public void testIsChildOfBinaryNoParent() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		assertFalse(isChildOfBinary);
	}

	@Test
	public void testIsChildOfBinaryHasParentButNotBinary() {
		addParentIdToContain(recordStorageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;
		assertParentIdWasRequestedFromDataGroup(recordTypeDataGroup);

		assertFalse(isChildOfBinary);
	}

	@Test
	public void testIsChildOfBinaryHasParentIsBinary() {
		addParentIdToContain(recordStorageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnParentIdBinary();

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;
		assertParentIdWasRequestedFromDataGroup(recordTypeDataGroup);

		assertTrue(isChildOfBinary);
	}

	private void setUpPresetDataGroupToReturnParentIdBinary() {
		DataGroupCheckCallsSpy groupToReturnFromStorage = new DataGroupCheckCallsSpy();
		DataGroupCheckCallsSpy parentIdChild = new DataGroupCheckCallsSpy();
		parentIdChild.atomicValuesToReturn.put("linkedRecordId", "binary");
		groupToReturnFromStorage.presetGroupChildrenToReturn.put("parentId", parentIdChild);
		recordStorageLightSpy.dataGroupReturnedFromSpy = groupToReturnFromStorage;
	}

	@Test
	public void testIsNotSearch() {
		addRecordInfoToContain(recordStorageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("NOTSearch");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean isSearchType = recordTypeHandler.representsTheRecordTypeDefiningSearches();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;

		assertRecordInfoWasRequestedFromDataGroup(recordTypeDataGroup);
		assertIdWasRequestedFromRecordInfo(recordTypeDataGroup);
		assertFalse(isSearchType);
	}

	private void addRecordInfoToContain(RecordStorageLightSpy recordStorgageSpy, String id) {
		addChildToGroupIdReturnedFromStorageSpy("recordInfo", id, recordStorgageSpy);
	}

	private void setUpPresetDataGroupToReturnRecordId(String recordId) {
		DataGroupCheckCallsSpy groupToReturnFromStorage = new DataGroupCheckCallsSpy();
		DataGroupCheckCallsSpy groupChild = new DataGroupCheckCallsSpy();
		groupChild.atomicValuesToReturn.put("id", recordId);
		groupToReturnFromStorage.presetGroupChildrenToReturn.put("recordInfo", groupChild);

		recordStorageLightSpy.dataGroupReturnedFromSpy = groupToReturnFromStorage;
	}

	private void assertRecordInfoWasRequestedFromDataGroup(
			DataGroupCheckCallsSpy recordTypeDataGroup) {
		assertEquals((recordTypeDataGroup.requestedFirstGroupWithNameInData.get(0)), "recordInfo");
	}

	private void assertIdWasRequestedFromRecordInfo(DataGroupCheckCallsSpy recordTypeDataGroup) {
		DataGroupCheckCallsSpy returnedRecordInfoGroup = recordTypeDataGroup.returnedChildrenGroups
				.get(0);
		assertEquals(returnedRecordInfoGroup.requestedFirstAtomicValue.get(0), "id");
	}

	@Test
	public void testIsSearch() {
		addRecordInfoToContain(recordStorageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("search");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean isSearchType = recordTypeHandler.representsTheRecordTypeDefiningSearches();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;

		assertRecordInfoWasRequestedFromDataGroup(recordTypeDataGroup);
		assertIdWasRequestedFromRecordInfo(recordTypeDataGroup);
		assertTrue(isSearchType);
	}

	@Test
	public void testIsNotRecordType() {
		addRecordInfoToContain(recordStorageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("NOTRecordType");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean isRecordType = recordTypeHandler.representsTheRecordTypeDefiningRecordTypes();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;

		assertRecordInfoWasRequestedFromDataGroup(recordTypeDataGroup);
		assertIdWasRequestedFromRecordInfo(recordTypeDataGroup);
		assertFalse(isRecordType);
	}

	@Test
	public void testIsRecordType() {
		addRecordInfoToContain(recordStorageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("recordType");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean isRecordType = recordTypeHandler.representsTheRecordTypeDefiningRecordTypes();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;

		assertRecordInfoWasRequestedFromDataGroup(recordTypeDataGroup);
		assertIdWasRequestedFromRecordInfo(recordTypeDataGroup);
		assertTrue(isRecordType);
	}

	@Test
	public void testHasSearch() {
		addChildToGroupIdReturnedFromStorageSpy("search", defaultRecordTypeId,
				recordStorageLightSpy);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		boolean hasSearch = recordTypeHandler.hasLinkedSearch();
		assertContainsWasRequestedForSearch();
		assertTrue(hasSearch);
	}

	private void assertContainsWasRequestedForSearch() {
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;
		assertEquals((recordTypeDataGroup.nameInDatasSentToContains.get(0)), "search");
	}

	@Test
	public void testHasNoSearch() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);
		assertFalse(recordTypeHandler.hasLinkedSearch());
	}

	@Test
	public void testGetSearchId() {
		addChildToGroupIdReturnedFromStorageSpy("search", defaultRecordTypeId,
				recordStorageLightSpy);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		String searchId = recordTypeHandler.getSearchId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorageLightSpy.dataGroupReturnedFromSpy;

		assertSearchWasRequestedFromDataGroup(recordTypeDataGroup);
		assertLinkedRecordIdWasRequestedAndReturned(recordTypeDataGroup, searchId);
	}

	private void assertSearchWasRequestedFromDataGroup(
			DataGroupCheckCallsSpy dataGroupReturnedFromSpy) {
		assertEquals((dataGroupReturnedFromSpy.requestedFirstGroupWithNameInData.get(0)), "search");
	}

	@Test(expectedExceptions = DataMissingException.class, expectedExceptionsMessageRegExp = ""
			+ "Unable to get searchId, no search exists")
	public void testGetSearchIdThrowsExceptionWhenMissing() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorageLightSpy,
						defaultRecordTypeId);

		recordTypeHandler.getSearchId();
	}

	@Test
	public void testGetImplentingRecordTypesNotAbstract() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorage, "someId");

		List<RecordTypeHandler> recordTypeHandlers = recordTypeHandler
				.getImplementingRecordTypeHandlers();
		assertTrue(recordTypeHandlers.isEmpty());
		recordStorage.MCR.assertMethodNotCalled("readList");
	}

	@Test
	public void testGetGroupGetImplentingRecordTypesNotAbstract() {
		DataGroupSpy dataGroup = createTopDataGroup();

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndDataGroup(null, recordStorage, dataGroup);

		List<RecordTypeHandler> recordTypeHandlers = recordTypeHandler
				.getImplementingRecordTypeHandlers();
		assertTrue(recordTypeHandlers.isEmpty());
		recordStorage.MCR.assertMethodNotCalled("readList");
	}

	@Test
	public void testGetImplentingRecordTypesAbstractButNoImplementingChildren() {
		setupForStorageAtomicValue(ABSTRACT, "true");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorage, "someId");

		List<RecordTypeHandler> recordTypeHandlers = recordTypeHandler
				.getImplementingRecordTypeHandlers();

		assertTrue(recordTypeHandlers.isEmpty());
		DataGroupSpy dataGroupMCR = getRecordTypeDataGroupReadFromStorage();
		assertCallMadeToStorageForAbstractRecordType(dataGroupMCR);
	}

	private void assertCallMadeToStorageForAbstractRecordType(DataGroupSpy dataGroupMCR) {
		dataGroupMCR.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, ABSTRACT);
		recordStorage.MCR.assertParameter("readList", 0, "type", "recordType");
		DataGroup filter = (DataGroup) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("readList", 0, "filter");
		assertEquals(filter, dataFactorySpy.MCR.getReturnValue("factorGroupUsingNameInData", 0));
	}

	@Test
	public void testDataGroupGetImplentingRecordTypesAbstractButNoImplementingChildren() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue(ABSTRACT, "true");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndDataGroup(null, recordStorage, dataGroup);

		List<RecordTypeHandler> recordTypeHandlers = recordTypeHandler
				.getImplementingRecordTypeHandlers();

		assertTrue(recordTypeHandlers.isEmpty());
		assertCallMadeToStorageForAbstractRecordType(dataGroup);
	}

	@Test
	public void testGetImplentingRecordTypesAbstractWithImplementingFirstLevelChildren() {
		setupForStorageAtomicValue(ABSTRACT, "true");

		List<RecordTypeHandlerSpy> list = new ArrayList<>();
		list.add(createTypeHandlerSpy("someId", "", true));
		list.add(createTypeHandlerSpy("id2", "someId", false));
		list.add(createTypeHandlerSpy("id3", "someOtherId", false));

		Iterator<RecordTypeHandlerSpy> iterator = list.iterator();
		recordTypeHandlerFactory.MRV.setDefaultReturnValuesSupplier("factorUsingDataGroup",
				(Supplier<RecordTypeHandlerSpy>) () -> iterator.next());

		recordStorage.createFakeGroupsInAnswerToList(list.size());

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordTypeHandlerFactory, recordStorage,
						"someId");

		List<RecordTypeHandler> returnedTypeHandlers = recordTypeHandler
				.getImplementingRecordTypeHandlers();

		assertRecordTypeHandlerFactoredForAllRecordTypesInStorage();

		RecordTypeHandlerSpy recordTypeHandlerSpy0 = (RecordTypeHandlerSpy) recordTypeHandlerFactory.MCR
				.getReturnValue("factorUsingDataGroup", 0);
		recordTypeHandlerSpy0.MCR.assertMethodWasCalled("hasParent");
		recordTypeHandlerSpy0.MCR.assertMethodNotCalled("getParentId");

		RecordTypeHandlerSpy recordTypeHandlerSpy1 = (RecordTypeHandlerSpy) recordTypeHandlerFactory.MCR
				.getReturnValue("factorUsingDataGroup", 1);
		recordTypeHandlerSpy1.MCR.assertMethodWasCalled("hasParent");
		recordTypeHandlerSpy1.MCR.assertMethodWasCalled("getParentId");

		RecordTypeHandlerSpy recordTypeHandlerSpy2 = (RecordTypeHandlerSpy) recordTypeHandlerFactory.MCR
				.getReturnValue("factorUsingDataGroup", 2);
		recordTypeHandlerSpy2.MCR.assertMethodWasCalled("hasParent");
		recordTypeHandlerSpy2.MCR.assertMethodWasCalled("getParentId");

		assertEquals(returnedTypeHandlers.size(), 1);
		assertSame(returnedTypeHandlers.get(0), recordTypeHandlerSpy1);
	}

	private RecordTypeHandlerSpy createTypeHandlerSpy(String recordId, String parentId,
			boolean isAbstract) {

		RecordTypeHandlerSpy recordTypeHandler = new RecordTypeHandlerSpy();

		recordTypeHandler.id = recordId;
		if (!parentId.equals("")) {
			recordTypeHandler.hasParent = true;
		}
		recordTypeHandler.isAbstract = isAbstract;
		recordTypeHandler.parentId = parentId;
		return recordTypeHandler;
	}

	private void assertRecordTypeHandlerFactoredForAllRecordTypesInStorage() {
		List<DataGroup> fakeDataGroups = recordStorage.dataGroupsForReadList;
		recordTypeHandlerFactory.MCR.assertParameters("factorUsingDataGroup", 0,
				fakeDataGroups.get(0));
		recordTypeHandlerFactory.MCR.assertParameters("factorUsingDataGroup", 1,
				fakeDataGroups.get(1));
		recordTypeHandlerFactory.MCR.assertParameters("factorUsingDataGroup", 2,
				fakeDataGroups.get(2));
	}

	@Test
	public void GetImplentingRecordTypesAbstractWithImplementingSeveralAbstarctLevels()
			throws Exception {

		setupForStorageAtomicValue(ABSTRACT, "true");
		String startRecordId = "someId";

		List<RecordTypeHandlerSpy> list = new ArrayList<>();
		RecordTypeHandlerSpy id5 = createTypeHandlerSpy("id5", "otherId1", false);
		list.add(id5);

		RecordTypeHandlerSpy id4 = createTypeHandlerSpy("id4", "id2", false);
		list.add(id4);

		RecordTypeHandlerSpy id3 = createTypeHandlerSpy("id3", "otherId1", true);
		id3.MRV.setReturnValues("getImplementingRecordTypeHandlers", List.of(List.of(id5)));
		list.add(id3);

		RecordTypeHandlerSpy id2 = createTypeHandlerSpy("id2", startRecordId, true);
		id2.MRV.setReturnValues("getImplementingRecordTypeHandlers", List.of(List.of(id4)));
		list.add(id2);

		RecordTypeHandlerSpy id6 = createTypeHandlerSpy("id6", startRecordId, false);
		list.add(id6);

		RecordTypeHandlerSpy someId = createTypeHandlerSpy(startRecordId, "", true);
		someId.MRV.setReturnValues("getImplementingRecordTypeHandlers", List.of(List.of(id2, id6)));
		list.add(someId);

		Iterator<RecordTypeHandlerSpy> iterator = list.iterator();
		recordTypeHandlerFactory.MRV.setDefaultReturnValuesSupplier("factorUsingDataGroup",
				(Supplier<RecordTypeHandlerSpy>) () -> iterator.next());

		recordStorage.createFakeGroupsInAnswerToList(list.size());

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordTypeHandlerFactory, recordStorage,
						startRecordId);

		List<RecordTypeHandler> returnedTypeHandlers = recordTypeHandler
				.getImplementingRecordTypeHandlers();

		assertEquals(returnedTypeHandlers.size(), 2);
		assertSame(returnedTypeHandlers.get(0), id4);
		assertSame(returnedTypeHandlers.get(1), id6);
	}

	@Test
	public void testGetListOfImplementingRecordTypeIds() {
		RecordTypeHandlerExtendedForTest rthft = new RecordTypeHandlerExtendedForTest(2);

		List<String> listOfIds = rthft.getListOfImplementingRecordTypeIds();
		assertEquals(listOfIds.size(), 2);
		assertEquals(listOfIds.get(0), "fakeRecordTypeIdFromRecordTypeHandlerSpy");
		assertEquals(listOfIds.get(1), "fakeRecordTypeIdFromRecordTypeHandlerSpy");
	}

	// from here
	@Test
	public void testShouldStoreInArchive() {
		setupForStorageAtomicValue("storeInArchive", "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertShouldStoreInArchive(dataGroup, false);
	}

	private void assertShouldStoreInArchive(DataGroupSpy dataGroupMCR, boolean expected) {
		assertEquals(recordTypeHandler.storeInArchive(), expected);
		dataGroupMCR.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "storeInArchive");
	}

	@Test
	public void testStoreInArchiveFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue("storeInArchive", "true");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertShouldStoreInArchive(dataGroup, true);
	}

	@Test
	public void testStoreInArchiveTrue() {
		setupForStorageAtomicValue("storeInArchive", "true");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(null,
				recordStorage, "someId");

		DataGroupSpy dataGroup = getRecordTypeDataGroupReadFromStorage();
		assertShouldStoreInArchive(dataGroup, true);
	}

	@Test
	public void testShouldStoreInArchiveFalseFromDataGroup() {
		DataGroupSpy dataGroup = setupDataGroupWithAtomicValue("userSuppliedId", "false");
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndDataGroup(null, recordStorage,
				dataGroup);

		assertShouldStoreInArchive(dataGroup, false);
	}
}

class RecordTypeHandlerExtendedForTest extends RecordTypeHandlerImp {
	List<RecordTypeHandler> implementingRecordTypesSpies = new ArrayList<>();

	public RecordTypeHandlerExtendedForTest(int numberOfSpies) {
		for (int j = 0; j < numberOfSpies; j++) {
			addRecordTypeHandlerSpyToList();
		}
	}

	private void addRecordTypeHandlerSpyToList() {
		RecordTypeHandlerSpy recordTypeHandlerSpy = new RecordTypeHandlerSpy();
		implementingRecordTypesSpies.add(recordTypeHandlerSpy);
	}

	@Override
	public List<RecordTypeHandler> getImplementingRecordTypeHandlers() {
		return implementingRecordTypesSpies;
	}

}
