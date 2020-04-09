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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;

public class RecordTypeHandlerTest {
	private OldRecordStorageSpy recordStorage;
	private RecordStorageLightSpy recordStorgageLightSpy;

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
		Set<String> recordPartWriteConstraints = recordTypeHandler.getRecordPartWriteConstraints();
		assertTrue(recordPartWriteConstraints.isEmpty());

	}

	@Test
	public void testGetRecordPartReadConstraintsOneReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		Set<String> recordPartReadConstraints = recordTypeHandler.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 1);
		assertTrue(recordPartReadConstraints.contains("organisationRoot"));

		Set<String> recordPartWriteConstraints = recordTypeHandler.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 1);
		assertTrue(recordPartWriteConstraints.contains("organisationRoot"));

	}

	@Test
	public void testGetRecordPartReadConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();

		String childWithConstraint = "divaOrganisationRoot";
		long childWithConstraintReadNumberOfTimes = storageSpy.ids.stream()
				.filter(id -> childWithConstraint.equals(id)).count();
		assertEquals(childWithConstraintReadNumberOfTimes, 1);
	}

	@Test
	public void testGetRecordPartWriteConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");

		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();
		String childWithConstraint = "divaOrganisationRoot";
		long childWithConstraintReadNumberOfTimes = storageSpy.ids.stream()
				.filter(id -> childWithConstraint.equals(id)).count();
		assertEquals(childWithConstraintReadNumberOfTimes, 1);
	}

	@Test
	public void testGetRecordPartReadConstraintsTwoReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadConstraint = 2;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		Set<String> recordPartReadConstraints = recordTypeHandler.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 2);
		assertTrue(recordPartReadConstraints.contains("organisationRoot"));
		assertTrue(recordPartReadConstraints.contains("showInPortal"));

		Set<String> recordPartWriteConstraints = recordTypeHandler.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 2);
		assertTrue(recordPartWriteConstraints.contains("organisationRoot"));
		assertTrue(recordPartWriteConstraints.contains("showInPortal"));

	}

	@Test
	public void testGetRecordPartReadConstraintsOnlyReadWriteConstraintsAreAdded() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadConstraint = 2;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		Set<String> recordPartReadConstraints = recordTypeHandler.getRecordPartReadConstraints();
		assertEquals(recordPartReadConstraints.size(), 2);
		assertTrue(recordPartReadConstraints.contains("organisationRoot"));
		assertTrue(recordPartReadConstraints.contains("showInPortal"));
		assertFalse(recordPartReadConstraints.contains("showInDefence"));

		Set<String> recordPartWriteConstraints = recordTypeHandler.getRecordPartWriteConstraints();
		assertEquals(recordPartWriteConstraints.size(), 3);
		assertTrue(recordPartWriteConstraints.contains("organisationRoot"));
		assertTrue(recordPartWriteConstraints.contains("showInPortal"));
		assertTrue(recordPartWriteConstraints.contains("showInDefence"));
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
		storageSpy.numberOfChildrenWithReadConstraint = 1;
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
		storageSpy.numberOfChildrenWithReadConstraint = 1;
		storageSpy.numberOfChildrenWithWriteConstraint = 0;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasRecordPartWriteConstraintsOneWriteConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildrenWithReadConstraint = 0;
		storageSpy.numberOfChildrenWithWriteConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Test
	public void testHasParent() {
		String id = "recordTypeWithParent";
		addParentIdToContain(recordStorgageLightSpy, id);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, id);

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
		String id = "recordTypeWithoutParent";

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, id);
		assertFalse(recordTypeHandler.hasParent());
	}

	private void addParentIdToContain(RecordStorageLightSpy recordStorgageSpy, String id) {
		List<String> childrenToContain = new ArrayList<>();
		childrenToContain.add("parentId");
		recordStorgageSpy.childrenToContainInDataGroup.put(id, childrenToContain);
	}

	@Test
	public void testGetParentId() {
		String id = "recordTypeWithParent";
		addParentIdToContain(recordStorgageLightSpy, id);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, id);

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
		String id = "recordTypeWithoutParent";

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, id);

		recordTypeHandler.getParentId();
	}

	@Test
	public void testIsChildOfBinaryNoParent() {
		String id = "recordTypeWithoutParent";
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, id);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		assertFalse(isChildOfBinary);
	}

	@Test
	public void testIsChildOfBinaryHasParentButNotBinary() {
		String id = "recordTypeWithParent";
		addParentIdToContain(recordStorgageLightSpy, id);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, id);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		DataGroupCheckCallsSpy recordTypeDataGroup = recordStorgageLightSpy.dataGroupReturnedFromSpy;
		assertParentIdWasRequestedFromDataGroup(recordTypeDataGroup);

		assertFalse(isChildOfBinary);
	}

	@Test
	public void testIsChildOfBinaryHasParentIsBinary() {
		String id = "recordTypeWithParent";
		addParentIdToContain(recordStorgageLightSpy, id);
		setUpPresetDataGroupToReturnParentIdBinary();

		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorgageLightSpy, id);

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
}
