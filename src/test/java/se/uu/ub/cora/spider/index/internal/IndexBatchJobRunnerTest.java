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
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;

public class IndexBatchJobRunnerTest {

	private SpiderDependencyProvider dependencyProvider;
	private DataGroupSpy dataGroup;

	@BeforeMethod
	public void setUp() {
		LoggerFactorySpy loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		dataGroup = new DataGroupSpy("indexBatchJob");
		Map<String, String> initInfo = new HashMap<>();
		// dependencyProvider = new SpiderDependencyProviderSpy(initInfo);
	}

	@Test
	public void testInit() {
		IndexBatchJobRunner batchRunner = new IndexBatchJobRunner(dependencyProvider, dataGroup);
		assertTrue(batchRunner instanceof Runnable);

		assertSame(batchRunner.getDataGroup(), dataGroup);
		assertSame(batchRunner.getDependencyProvider(), dependencyProvider);

		// dependencyProvider.
	}

}
