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

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataRecordSpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class IndexBatchJobStorerSpy implements BatchJobStorer {

	public IndexBatchJob indexBatchJob;
	public long numberOfIndexed;
	public List<IndexError> errors = new ArrayList<>();
	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public void store(IndexBatchJob indexBatchJob) {
		MCR.addCall("indexBatchJob", indexBatchJob);

		this.indexBatchJob = indexBatchJob;
		numberOfIndexed = indexBatchJob.numOfProcessedRecords;
		errors.addAll(indexBatchJob.errors);
	}

	@Override
	public DataRecord create(IndexBatchJob indexBatchJob) {
		MCR.addCall("indexBatchJob", indexBatchJob);
		DataRecordSpy dataRecordSpy = new DataRecordSpy(new DataGroupSpy("someSpyDataGroup"));
		MCR.addReturned(dataRecordSpy);
		return dataRecordSpy;

	}

}
