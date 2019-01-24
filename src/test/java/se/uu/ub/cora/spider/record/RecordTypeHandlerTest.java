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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;

public class RecordTypeHandlerTest {
	private RecordStorage recordStorage;

	@BeforeMethod
	public void setUp() {
		recordStorage = new RecordStorageSpy();
	}

	@Test
	public void testAbstract() {
		String id = "abstract";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertTrue(recordTypeHandler.isAbstract());
	}

	@Test
	public void testNotAbstract() {
		String id = "spyType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.isAbstract());
	}

	@Test
	public void testShouldAutogenerateId() {
		String id = "spyType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertTrue(recordTypeHandler.shouldAutoGenerateId());
	}

	@Test
	public void testShouldNotAutogenerateId() {
		String id = "otherType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.shouldAutoGenerateId());
	}

	@Test
	public void testGetNewMetadataId() {
		String id = "otherType";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertEquals(recordTypeHandler.getNewMetadataId(), "otherTypeNew");
	}

	@Test
	public void testPublic() {
		String id = "public";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertTrue(recordTypeHandler.isPublicForRead());
	}

	@Test
	public void testNotPublic() {
		String id = "notPublic";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.isPublicForRead());
	}

	@Test
	public void testPublicMissing() {
		String id = "publicMissing";
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, id);
		assertFalse(recordTypeHandler.isPublicForRead());
	}
}
