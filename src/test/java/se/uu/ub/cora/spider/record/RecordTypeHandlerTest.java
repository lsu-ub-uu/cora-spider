/*
 * Copyright 2016, 2019, 2020 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;

public class RecordTypeHandlerTest {
	private OldRecordStorageSpy recordStorage;
	private RecordStorageLightSpy recordStorgageLightSpy;
	private String defaultRecordTypeId = "someRecordType";

	@BeforeMethod
	public void setUp() {
		recordStorage = new OldRecordStorageSpy();
		recordStorgageLightSpy = new RecordStorageLightSpy();
	}

	@Test
	public void testAbstract() {
		String id = "abstract";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertTrue(recordTypeHandler.isAbstract());
	}

	@Test
	public void testNotAbstract() {
		String id = "spyType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.isAbstract());
	}

	@Test
	public void testShouldAutogenerateId() {
		String id = "spyType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertTrue(recordTypeHandler.shouldAutoGenerateId());
	}

	@Test
	public void testShouldNotAutogenerateId() {
		String id = "otherType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.shouldAutoGenerateId());
	}

	@Test
	public void testGetNewMetadataId() {
		String id = "otherType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertEquals(recordTypeHandler.getNewMetadataId(), "otherTypeNew");
	}

	@Test
	public void testPublic() {
		String id = "public";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertTrue(recordTypeHandler.isPublicForRead());
	}

	@Test
	public void testNotPublic() {
		String id = "notPublic";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.isPublicForRead());
	}

	@Test
	public void testPublicMissing() {
		String id = "publicMissing";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.isPublicForRead());
	}

	@Test
	public void testGetMetadataGroup() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, "book");
		DataGroup metadataGroup = recordTypeHandler.getMetadataGroup();
		assertSame(metadataGroup, recordStorage.readDataGroup);
	}

	@Test
	public void testGetMetadataGroupTwiceReturnsSameInstance() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, "book");
		DataGroup metadataGroup = recordTypeHandler.getMetadataGroup();
		assertSame(metadataGroup, recordStorage.readDataGroup);
		DataGroup metadataGroup2 = recordTypeHandler.getMetadataGroup();
		assertSame(metadataGroup, metadataGroup2);
	}

	@Test
	public void testGetRecordPartReadConstraintsNOReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		Set<String> recordPartReadConstraints = recordTypeHandler.getRecordPartReadConstraints();
		assertEquals(storageSpy.type, "metadataGroup");
		assertEquals(storageSpy.id, "organisation");
		assertTrue(recordPartReadConstraints.isEmpty());
		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertTrue(recordPartWriteConstraints.isEmpty());

		Set<String> recordPartCreateWriteConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertTrue(recordPartCreateWriteConstraints.isEmpty());
	}

	@Test
	public void testGetRecordPartReadConstraintsOneReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		Set<String> recordPartReadConstraints = recordTypeHandler.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 1);
		assertTrue(recordPartReadConstraints.contains("organisationRoot"));

		Set<Constraint> recordPartWriteConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 1);
		Constraint constraint = recordPartWriteConstraints.iterator().next();
		assertEquals(constraint.getNameInData(), "organisationRoot");

		Set<String> recordPartWriteCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartWriteCreateConstraints.size(), 1);
		assertTrue(recordPartWriteCreateConstraints.contains("organisationRoot2"));
	}

	@Test
	public void testGetRecordPartReadConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 3);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisation");
		storageSpy.MCR.assertParameters("read", 2, "metadataTextVariable", "divaOrganisationRoot");
	}

	@Test
	public void testGetRecordPartWriteConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");

		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 3);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisation");
		storageSpy.MCR.assertParameters("read", 2, "metadataTextVariable", "divaOrganisationRoot");
	}

	@Test
	public void testGetRecordPartCreateConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		recordTypeHandler.getRecordPartCreateWriteConstraints();
		recordTypeHandler.getRecordPartCreateWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 3);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisationNew");
		storageSpy.MCR.assertParameters("read", 2, "metadataTextVariable", "divaOrganisationRoot2");
	}

	@Test
	public void testGetRecordPartReadConstraintsTwoReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 2;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		Set<String> recordPartReadForUpdateConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(recordPartReadForUpdateConstraints.size(), 2);
		assertTrue(recordPartReadForUpdateConstraints.contains("organisationRoot"));
		assertTrue(recordPartReadForUpdateConstraints.contains("showInPortal"));

		Set<Constraint> recordPartWriteForUpdateConstraints = recordTypeHandler
				.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteForUpdateConstraints.size(), 2);

		assertTrue(containsConstraintWithNameInData(recordPartWriteForUpdateConstraints,
				"showInPortal"));
		assertTrue(containsConstraintWithNameInData(recordPartWriteForUpdateConstraints,
				"organisationRoot"));

		Set<String> recordPartCreateConstraints = recordTypeHandler
				.getRecordPartCreateWriteConstraints();
		assertEquals(recordPartCreateConstraints.size(), 2);
		assertTrue(recordPartCreateConstraints.contains("organisationRoot2"));
		assertTrue(recordPartCreateConstraints.contains("showInPortal2"));
	}

	@Test
	public void testGetRecordPartReadConstraintsOnlyReadWriteConstraintsAreAdded() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 2;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
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
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertFalse(recordTypeHandler.hasRecordPartReadConstraint());
	}

	@Test
	public void testHasRecordPartReadConstraintsOneConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartReadConstraint());
	}

	@Test
	public void testHasRecordPartWriteConstraintsNoConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertFalse(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasRecordPartWriteConstraintsOneReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfChildrenWithWriteConstraint = 0;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasRecordPartWriteConstraintsOneWriteConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 0;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasRecordPartCreateConstraintsNoConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertFalse(recordTypeHandler.hasRecordPartCreateConstraint());
	}

	@Test
	public void testHasRecordPartCreateConstraintsOneReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;
		storageSpy.numberOfChildrenWithWriteConstraint = 0;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartCreateConstraint());
	}

	@Test
	public void testHasRecordPartCreateConstraintsOneWriteConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadWriteConstraint = 0;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartCreateConstraint());
	}

	@Test
	public void testHasParent() {
		addParentIdToContain(recordStorgageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean hasParent = recordTypeHandler.hasParent();
		assertContainsWasRequestedForParentId();
		assertTrue(hasParent);
	}

	private void assertContainsWasRequestedForParentId() {
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;
		assertEquals((recordTypeDataGroup.nameInDatasSentToContains.get(0)), "parentId");
	}

	@Test
	public void testHasNoParent() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);
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
		addParentIdToContain(recordStorgageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		String parentId = recordTypeHandler.getParentId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;

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
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		recordTypeHandler.getParentId();
	}

	@Test
	public void testIsChildOfBinaryNoParent() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		assertFalse(isChildOfBinary);
	}

	@Test
	public void testIsChildOfBinaryHasParentButNotBinary() {
		addParentIdToContain(recordStorgageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;
		assertParentIdWasRequestedFromDataGroup(recordTypeDataGroup);

		assertFalse(isChildOfBinary);
	}

	@Test
	public void testIsChildOfBinaryHasParentIsBinary() {
		addParentIdToContain(recordStorgageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnParentIdBinary();

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;
		assertParentIdWasRequestedFromDataGroup(recordTypeDataGroup);

		assertTrue(isChildOfBinary);
	}

	private void setUpPresetDataGroupToReturnParentIdBinary() {
		DataGroupCheckCallsSpy groupToReturnFromStorage = new DataGroupCheckCallsSpy();
		DataGroupCheckCallsSpy parentIdChild = new DataGroupCheckCallsSpy();
		parentIdChild.atomicValuesToReturn.put("linkedRecordId", "binary");
		groupToReturnFromStorage.presetGroupChildrenToReturn.put("parentId", parentIdChild);
		recordStorgageLightSpy.dataGroupReturnedFromSpy = groupToReturnFromStorage;
	}

	@Test
	public void testIsNotSearch() {
		addRecordInfoToContain(recordStorgageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("NOTSearch");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean isSearchType = recordTypeHandler.representsTheRecordTypeDefiningSearches();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;

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

		recordStorgageLightSpy.dataGroupReturnedFromSpy = groupToReturnFromStorage;
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
		addRecordInfoToContain(recordStorgageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("search");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean isSearchType = recordTypeHandler.representsTheRecordTypeDefiningSearches();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;

		assertRecordInfoWasRequestedFromDataGroup(recordTypeDataGroup);
		assertIdWasRequestedFromRecordInfo(recordTypeDataGroup);
		assertTrue(isSearchType);
	}

	@Test
	public void testIsNotRecordType() {
		addRecordInfoToContain(recordStorgageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("NOTRecordType");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean isRecordType = recordTypeHandler.representsTheRecordTypeDefiningRecordTypes();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;

		assertRecordInfoWasRequestedFromDataGroup(recordTypeDataGroup);
		assertIdWasRequestedFromRecordInfo(recordTypeDataGroup);
		assertFalse(isRecordType);
	}

	@Test
	public void testIsRecordType() {
		addRecordInfoToContain(recordStorgageLightSpy, defaultRecordTypeId);
		setUpPresetDataGroupToReturnRecordId("recordType");
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean isRecordType = recordTypeHandler.representsTheRecordTypeDefiningRecordTypes();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;

		assertRecordInfoWasRequestedFromDataGroup(recordTypeDataGroup);
		assertIdWasRequestedFromRecordInfo(recordTypeDataGroup);
		assertTrue(isRecordType);
	}

	@Test
	public void testHasSearch() {
		addChildToGroupIdReturnedFromStorageSpy("search", defaultRecordTypeId,
				recordStorgageLightSpy);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		boolean hasSearch = recordTypeHandler.hasLinkedSearch();
		assertContainsWasRequestedForSearch();
		assertTrue(hasSearch);
	}

	private void assertContainsWasRequestedForSearch() {
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;
		assertEquals((recordTypeDataGroup.nameInDatasSentToContains.get(0)), "search");
	}

	@Test
	public void testHasNoSearch() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);
		assertFalse(recordTypeHandler.hasLinkedSearch());
	}

	@Test
	public void testGetSearchId() {
		addChildToGroupIdReturnedFromStorageSpy("search", defaultRecordTypeId,
				recordStorgageLightSpy);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		String searchId = recordTypeHandler.getSearchId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;

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
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, defaultRecordTypeId);

		recordTypeHandler.getSearchId();
	}
}
