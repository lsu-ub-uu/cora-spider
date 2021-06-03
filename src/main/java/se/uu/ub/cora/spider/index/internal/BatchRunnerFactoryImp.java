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

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.index.BatchRunner;
import se.uu.ub.cora.spider.index.BatchRunnerFactory;

public class BatchRunnerFactoryImp implements BatchRunnerFactory {

	private SpiderDependencyProvider dependencyProvider;

	public BatchRunnerFactoryImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public BatchRunner factor(IndexBatchJob indexBatchJob) {
		IndexBatchJobStorer indexBatchJobStorer = new IndexBatchJobStorer(dependencyProvider,
				new DataGroupHandlerForIndexBatchJobImp());
		return new IndexBatchJobRunner(dependencyProvider, indexBatchJobStorer, indexBatchJob);
	}

	public SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

}
