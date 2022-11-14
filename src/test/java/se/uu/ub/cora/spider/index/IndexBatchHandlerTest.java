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
package se.uu.ub.cora.spider.index;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.index.internal.IndexBatchHandlerImp;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.storage.Filter;

public class IndexBatchHandlerTest {

	private BatchRunnerFactorySpy runnerFactory;
	private IndexBatchHandler batchHandler;

	@BeforeMethod
	public void setUp() {
		runnerFactory = new BatchRunnerFactorySpy();
		batchHandler = IndexBatchHandlerImp.usingBatchRunnerFactory(runnerFactory);
	}

	@Test
	public void testInit() throws InterruptedException {
		IndexBatchJob batchJob = new IndexBatchJob("someRecordType", 45, new Filter());

		batchHandler.runIndexBatchJob(batchJob);

		TimeUnit.SECONDS.sleep(2);

		BatchRunnerSpy batchRunnerSpy = runnerFactory.batchRunnerSpy;
		assertTrue(batchRunnerSpy.runWasCalled);
		assertSame(runnerFactory.indexBatchJob, batchJob);

	}

}
