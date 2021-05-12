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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.index.BatchRunner;
import se.uu.ub.cora.storage.RecordStorage;

public class IndexBatchJobRunner implements BatchRunner, Runnable {

	private DataGroup dataGroup;
	private SpiderDependencyProvider dependencyProvider;

	public IndexBatchJobRunner(SpiderDependencyProvider dependencyProvider, DataGroup dataGroup) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroup = dataGroup;
	}

	@Override
	public void run() {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();

		// hur veta om abstract list?
		recordStorage.readList(null, dataGroup);

		// loop records, send each to indexing
		// list records as specified in indexBatchJob, in groups of 10
		// read indexBatchJob (to see if it should be paused)

		// WorkOrderExecutor contains code that indexes 1 record, break out to new class, use from
		// there and in here

		// update indexBatchJob with info about the ten just indexed
		// get next group of 10 repeat

		// when finished write status to indexBatchJob
	}

	DataGroup getDataGroup() {
		return dataGroup;
	}

	SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

}
