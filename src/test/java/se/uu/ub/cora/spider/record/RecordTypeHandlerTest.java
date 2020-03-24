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

import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;

public class RecordTypeHandlerTest {
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	public void setUp() {
		recordStorage = new RecordStorageSpy();
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
		storageSpy.numberOfChildsWithConstraint = 1;
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
		storageSpy.numberOfChildsWithConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");

		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartReadConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();
		recordTypeHandler.getRecordPartWriteConstraints();
		int numOfTimesReadWhenConstraintsOnlyReadOnce = 3;
		assertEquals(storageSpy.types.size(), numOfTimesReadWhenConstraintsOnlyReadOnce);

	}

	@Test
	public void testGetRecordPartReadConstraintsTwoReadConstraint() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildsWithConstraint = 2;
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
		storageSpy.numberOfChildsWithConstraint = 3;
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
		storageSpy.numberOfChildsWithConstraint = 1;
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
	public void testHasRecordPartWriteConstraintsOneConstraints() {
		RecordTypeHandlerStorageSpy storageSpy = new RecordTypeHandlerStorageSpy();
		storageSpy.numberOfChildsWithConstraint = 1;
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(storageSpy, "organisation");
		assertTrue(recordTypeHandler.hasRecordPartWriteConstraint());
	}
}
