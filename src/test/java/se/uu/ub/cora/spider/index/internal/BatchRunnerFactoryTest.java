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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.index.BatchRunnerFactory;

public class BatchRunnerFactoryTest {

	private SpiderDependencyProviderSpy dependencyProvider;

	@Test
	public void testFactor() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		BatchRunnerFactory factory = new BatchRunnerFactoryImp(dependencyProvider);
		DataGroup dataGroupFilter = new DataGroupSpy("indexBatchJob");
		IndexBatchJob indexBatchJob = new IndexBatchJob("someRecordType", 10, dataGroupFilter);

		IndexBatchJobRunner runner = (IndexBatchJobRunner) factory.factor(indexBatchJob);

		assertSame(runner.getIndexBatchJob(), indexBatchJob);
		assertSame(runner.getDependencyProvider(), dependencyProvider);

		assertCorrectIndexBatchJobStorer(runner);
	}

	private void assertCorrectIndexBatchJobStorer(IndexBatchJobRunner runner) {
		IndexBatchJobStorer batchJobStorer = (IndexBatchJobStorer) runner.getBatchJobStorer();
		assertTrue(batchJobStorer instanceof IndexBatchJobStorer);
		assertTrue(batchJobStorer
				.getDataGroupHandlerForIndexBatchJob() instanceof DataGroupHandlerForIndexBatchJobImp);
		assertEquals(batchJobStorer.getDependencyProvider(), dependencyProvider);
	}

}
