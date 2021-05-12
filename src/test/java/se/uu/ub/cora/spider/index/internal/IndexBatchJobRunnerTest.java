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

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.internal.RecordStorageSpy;

public class IndexBatchJobRunnerTest {

	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupSpy dataGroupFilter;
	private RecordStorageSpy recordStorage;
	private IndexBatchJob indexBatchJob;

	@BeforeMethod
	public void setUp() {
		LoggerFactorySpy loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);

		dataGroupFilter = new DataGroupSpy("filter");
		indexBatchJob = new IndexBatchJob("someRecordType", dataGroupFilter);

		Map<String, String> initInfo = new HashMap<>();
		dependencyProvider = new SpiderDependencyProviderSpy(initInfo);
		recordStorage = new RecordStorageSpy();
		dependencyProvider.recordStorage = recordStorage;
	}

	@Test
	public void testInit() {
		IndexBatchJobRunner batchRunner = new IndexBatchJobRunner(dependencyProvider,
				indexBatchJob);
		assertTrue(batchRunner instanceof Runnable);

		assertSame(batchRunner.getIndexBatchJob(), indexBatchJob);
		assertSame(batchRunner.getDependencyProvider(), dependencyProvider);

	}

	@Test
	public void testRun() {
		IndexBatchJobRunner batchRunner = new IndexBatchJobRunner(dependencyProvider,
				indexBatchJob);
		batchRunner.run();
		recordStorage.MCR.assertParameter("readList", 0, "type", indexBatchJob.recordType);
		recordStorage.MCR.assertParameter("readList", 0, "filter", dataGroupFilter);

		// dependencyProvider.recordStorage;
	}

}
