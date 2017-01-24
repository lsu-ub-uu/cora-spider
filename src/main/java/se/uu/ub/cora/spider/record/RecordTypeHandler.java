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

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class RecordTypeHandler {
	private DataGroup recordType;
	private static final String RECORD_TYPE = "recordType";

	private RecordTypeHandler(RecordStorage recordStorage, String id) {
		recordType = recordStorage.read(RECORD_TYPE, id);
	}

	public static RecordTypeHandler usingRecordStorageAndRecordTypeId(RecordStorage recordStorage,
			String id) {
		return new RecordTypeHandler(recordStorage, id);
	}

	public boolean isAbstract() {
		String abstractInRecordTypeDefinition = getAbstractFromRecordTypeDefinition();
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private String getAbstractFromRecordTypeDefinition() {
		return recordType.getFirstAtomicValueWithNameInData("abstract");
	}

	public boolean shouldAutoGenerateId() {
		String userSuppliedId = recordType.getFirstAtomicValueWithNameInData("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	public String getNewMetadataId() {
		DataGroup newMetadataGroup = recordType.getFirstGroupWithNameInData("newMetadataId");
		return newMetadataGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

}
