/*
 * Copyright 2021 Uppsala University Library
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
package se.uu.ub.cora.spider.record.internal;

import org.testng.annotations.Test;

public class IndexBatchJobCreatorOrOtherBetterNameTest {

	@Test
	public void testCreateAndStoreRecord() {
		RecordListIndexer batchJob = new IndexBatchJobCreatorOrOtherBetterName();
		// DataGroup filter = createFilter();
		// batchJob.createAndStoreRecord(null, null, indexBatchJob);
	}

	// {
	// "name": "indexBatchJob",
	// "children": [
	// {
	// "name": "recordInfo",
	// "children": [
	// {
	// "name": "id",
	// "value": "indexBatchJob:001"
	// },
	// {
	// "name": "type",
	// "children": [
	// {
	// "name": "linkedRecordType",
	// "value": "recordType"
	// },
	// {
	// "name": "linkedRecordId",
	// "value": "indexBatchJob"
	// }
	// ]
	// }
	// ]
	// },
	// {
	// "name": "recordTypeToIndex",
	// "children": [
	// {
	// "name": "linkedRecordType",
	// "value": "recordType"
	// },
	// {
	// "name": "linkedRecordId",
	// "value": "person"
	// }
	// ]
	// },
	// {
	// "name": "status",
	// "value": "active"
	// },
	// {
	// "name": "filter",
	// "children": [
	// {
	// "name": "include",
	// "children": [
	// {
	// "name": "includePart",
	// "children": [
	// {
	// "name": "domain",
	// "value": "uu"
	// }
	// ]
	// }
	// ]
	// },{
	// "name": "start",
	// "value": "0"
	// },{
	// "name": "end",
	// "value": "200"
	// }
	// ]
	// }
	// ]
	// }
}
