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
package se.uu.ub.cora.spider.extendedfunctionality.internal;

import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.record.internal.IndexBatchJobCreatorOrOtherBetterName;

public class IndexBatchJobCreatorOrOtherBetterNameTest {

	@Test
	public void testCreateAndStoreRecord() {
		IndexBatchJobCreatorOrOtherBetterName batchJob = new IndexBatchJobCreatorOrOtherBetterName();
		// DataGroup indexBatchJob = createDataGroup();
		// batchJob.createAndStoreRecord(null, null, indexBatchJob);
	}

	public DataGroup createDataGroup() {
		// spånar lite hur datagruppen ska se ut - kanske inte alls behöver allt detta i
		// testet
		DataGroupSpy indexBatchJob = new DataGroupSpy("indexBatchJob");
		DataGroupSpy recordTypeToIndex = new DataGroupSpy("recordTypeToIndex", "recordType",
				"someRecordType");
		indexBatchJob.addChild(recordTypeToIndex);

		indexBatchJob.addChild(new DataAtomicSpy("status", "active"));

		// filter måste väl vara en del av indexBatchJob?
		DataGroupSpy filter = new DataGroupSpy("filter");
		DataGroupSpy include = new DataGroupSpy("include");
		DataGroupSpy includePart = new DataGroupSpy("includePart");
		includePart.addChild(new DataAtomicSpy("domain", "uu"));
		include.addChild(includePart);
		filter.addChild(include);

		filter.addChild(new DataAtomicSpy("start", "0"));
		filter.addChild(new DataAtomicSpy("end", "200"));

		indexBatchJob.addChild(filter);

		return indexBatchJob;
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
