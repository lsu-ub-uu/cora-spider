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

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.index.BatchRunnerFactory;
import se.uu.ub.cora.storage.Filter;

public class BatchRunnerFactoryTest {
	private DataFactorySpy dataFactorySpy;
	private SpiderDependencyProviderOldSpy dependencyProvider;

	@Test
	public void testFactor() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		dependencyProvider = new SpiderDependencyProviderOldSpy(new HashMap<>());
		BatchRunnerFactory factory = new BatchRunnerFactoryImp(dependencyProvider);
		IndexBatchJob indexBatchJob = new IndexBatchJob("someRecordType", 10, new Filter());

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
