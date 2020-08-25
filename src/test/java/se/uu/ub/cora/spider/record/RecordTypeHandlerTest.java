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
import se.uu.ub.cora.data.DataAttributeProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAttributeFactorySpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;

public class RecordTypeHandlerTest {
	private OldRecordStorageSpy recordStorage;
	private RecordStorageLightSpy recordStorageLightSpy;
	private String defaultRecordTypeId = "someRecordType";
	private RecordTypeHandler recordTypeHandler;
	private DataAttributeFactorySpy dataAttributeFactorySpy;

	@BeforeMethod
	public void setUp() {
		recordStorage = new OldRecordStorageSpy();
		recordStorageLightSpy = new RecordStorageLightSpy();
		dataAttributeFactorySpy = new DataAttributeFactorySpy();
		DataAttributeProvider.setDataAttributeFactory(dataAttributeFactorySpy);
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
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");

		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadConstraints();
		assertEquals(storageSpy.type, "metadataGroup");
		assertEquals(storageSpy.id, "organisation");
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
		recordTypeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(storageSpy,
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

		assertEquals(dataAttributeFactorySpy.nameInDataList.get(0), "type");
		assertEquals(dataAttributeFactorySpy.valueList.get(0), "default");

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

	@Test
	public void testGetRecordPartReadConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;

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
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;

		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 3);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisation");
		storageSpy.MCR.assertParameters("read", 2, "metadataTextVariable", "divaOrganisationRoot");
	}

	@Test
	public void testGetRecordPartCreateConstraintsReturnsSameInstance() {
		RecordTypeHandlerStorageSpy storageSpy = setUpHandlerWithStorageSpyUsingTypeId(
				"organisation");
		storageSpy.numberOfChildrenWithReadWriteConstraint = 1;

		recordTypeHandler.getRecordPartCreateWriteConstraints();
		recordTypeHandler.getRecordPartCreateWriteConstraints();

		storageSpy.MCR.assertNumberOfCallsToMethod("read", 3);
		storageSpy.MCR.assertParameters("read", 0, "recordType", "organisation");
		storageSpy.MCR.assertParameters("read", 1, "metadataGroup", "organisationNew");
		storageSpy.MCR.assertParameters("read", 2, "metadataTextVariable", "divaOrganisationRoot2");
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
		addParentIdToContain(recordStorageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);
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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

		recordTypeHandler.getParentId();
	}

	@Test
	public void testIsChildOfBinaryNoParent() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

		boolean isChildOfBinary = recordTypeHandler.isChildOfBinary();

		assertContainsWasRequestedForParentId();
		assertFalse(isChildOfBinary);
	}

	@Test
	public void testIsChildOfBinaryHasParentButNotBinary() {
		addParentIdToContain(recordStorageLightSpy, defaultRecordTypeId);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);
		assertFalse(recordTypeHandler.hasLinkedSearch());
	}

	@Test
	public void testGetSearchId() {
		addChildToGroupIdReturnedFromStorageSpy("search", defaultRecordTypeId,
				recordStorageLightSpy);
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

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
				.usingRecordStorageAndRecordTypeId(recordStorageLightSpy, defaultRecordTypeId);

		recordTypeHandler.getSearchId();
	}
}
