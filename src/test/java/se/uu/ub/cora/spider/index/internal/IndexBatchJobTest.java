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
package se.uu.ub.cora.spider.index.internal;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.storage.Filter;

public class IndexBatchJobTest {

	@Test
	public void testInit() {
		String recordType = "someRecordType";
		long totalNumberToIndex = 53;
		Filter filter = new Filter();
		DataGroupSpy filterAsData = new DataGroupSpy();

		IndexBatchJob indexBatchJob = new IndexBatchJob(recordType, totalNumberToIndex,
				filterAsData, filter);

		assertEquals(indexBatchJob.recordTypeToIndex, recordType);
		assertEquals(indexBatchJob.totalNumberToIndex, totalNumberToIndex);
		assertEquals(indexBatchJob.filterAsData, filterAsData);
		assertEquals(indexBatchJob.filter, filter);
		assertEquals(indexBatchJob.numberOfProcessedRecords, 0);
		assertEquals(indexBatchJob.status, "started");

	}
}
