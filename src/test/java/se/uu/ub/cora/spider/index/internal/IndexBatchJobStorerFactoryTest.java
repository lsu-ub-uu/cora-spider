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

import java.util.Collections;

import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;

public class IndexBatchJobStorerFactoryTest {

	@Test
	public void testInit() {
		SpiderDependencyProvider dependencyProvider = new SpiderDependencyProviderSpy(
				Collections.emptyMap());
		BatchJobConverterFactory converterFactory = new BatchJobConverterFactorySpy();

		BatchJobStorerFactory factory = new IndexBatchJobStorerFactory(dependencyProvider,
				converterFactory);

		IndexBatchJobStorer storer = (IndexBatchJobStorer) factory.factor();
		assertSame(storer.getDependencyProvider(), dependencyProvider);

		assertSame(storer.getBatchJobConverterFactory(), converterFactory);
	}

}
